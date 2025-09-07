package havsale.Core;

import ReflectorRuntime.Reflector;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jsoner.JSONReader;
import jsoner.JSONWriter;
import rwservice.RWService;

/**
 * Sistemin kendisidir; şu vazîfeleri üstlenir:<br>
 * Diğer alt bileşenlerin başlatılmasını, tetiklenmesini ve idâresini üstlenir<br>
 * Verilen adrese dayanarak sistemi yükler; Sistem yüklemesi otomatik yapılır
 * ve şunları içerir:<br>:
 * - Dizindeki "havsaleConfs.json" dosyasıyla yapılandırmayı yüklemeyi tetikler<br>
 * - Dizindeki verilerin taranarak, yüklenmesini tetikler<br>
 * Kullanıcın yaptığı CRUD işlemlerine cevâp verir<br>
 * @author Mehmet Akif SOLAK
 * @version 1.0.0
 */
public class Systemer implements IDataAccess{
    private DataB dBase;
    private static final String CONF_FILE_NAME = "havsaleConfs.json";
    private static final String LOG_FILE_NAME = "havsale.log";
    private OutputStream streamLog;
    private HavsaleConfiguration confs = null;
    private JSONWriter wrt;
    private EntityRelationer entityRelationer;

    /**
     * Verilen dizin yoluna göre sistemi başlatır.<br>
     * Eğer mevcut dizinde bir Havsale veritabanı yoksa, oluşturulur<br>
     * Şu durumlarda {@code IllegalArgumentException} fırlatılır:<br>
     * - Verilen {@code path} {@code null} ise<br>
     * - Verilen {@code path} bir dosyayı işâret ediyorsa<br>
     * - Verilen {@code path} dizini için gerekli izinler yoksa
     * @param path Havsale veritabanı çalışma dizini
     */
    public Systemer(String path, OutputStream streamForLog) throws IllegalArgumentException{
        if(streamForLog != null)
            setStreamForLog(streamForLog);
        startingControl(path);
        loadSystem();
    }
    /**
     * Verilen dizin yoluna göre sistemi başlatır.<br>
     * Eğer mevcut dizinde bir Havsale veritabanı yoksa, oluşturulur<br>
     * Şu durumlarda {@code IllegalArgumentException} fırlatılır:<br>
     * - Verilen {@code path} {@code null} ise<br>
     * - Verilen {@code path} bir dosyayı işâret ediyorsa<br>
     * - Verilen {@code path} dizini için gerekli izinler yoksa
     * @param path Havsale veritabanı çalışma dizini
     */
    public Systemer(String path) throws IllegalArgumentException{
        this(path, RWService.getService().getOutputStreamForFile(path, LOG_FILE_NAME, true, true));
    }

// İŞLEM METOTLARI:
    /**
     * Kayıtların (logs) bir yere yazılması için akış belirlemeyi sağlar<br>
     * Bu yapıldığında kayıtlar verilen akışa aktarılır
     * @param streamForLog Kayıtların (logs) akıtılacağı bir akış
     */
    public void setStreamForLog(OutputStream streamForLog){
        if(streamForLog == this.streamLog)
            return;
        if(this.streamLog != null){
            writeLog("Kayıt (log) akışı değiştiriliyor..");
        }
        this.streamLog = streamForLog;
        if(streamForLog != null)
            writeLog("Kayıt (log) için hedef akış değiştirildi.");
    }
    /**
     * Kayıtların (logs) bir dizine yazılması için akış belirlemeyi sağlar<br>
     * Bu yapıldığında kayıtlar verilen dizinde "havsale.logs" isimli dosyaya
     * yazılır<br>
     * Eğer dosyaya yazma izni yoksa veriler dosyaya yazılmaz<br>
     * @param path Kayıtların (logs) akıtılacağı bir dosya
     */
    public void setStreamForLogToFile(String path){
        OutputStream outStream = dBase.getIoServ().getOutputStreamForFile(path, true, true);
        if(outStream != null)
            setStreamForLog(streamLog);
    }
    @Override
    public boolean addEntity(Object entity){
        return addUpdateEntityMain(entity, true);
    }
    @Override
    public boolean deleteEntity(Object entity){
        EntityConfiguration entConfs = checkAndReturnEntityConfiguration(entity);
        if(entConfs == null)
            return false;
        Object primaryKeyValue = HavsaleRelationHandler.getIdValue(entity,
            entConfs, getConfs().getCodingStyleInner());
        boolean isSuccessful = dBase.deleteEntity(entity, primaryKeyValue);
        writeLog(entity.getClass().getName(), primaryKeyValue, 'd', isSuccessful);
        return isSuccessful;
    }
    @Override
    public boolean updateEntity(Object entity){
        return addUpdateEntityMain(entity, false);
    }
    @Override
    public <T> T getEntity(Class<T> entityClass, Object valueOfPrimaryKey){
        if(entityClass == null || valueOfPrimaryKey == null)
            return null;
        return dBase.getEntity(entityClass, valueOfPrimaryKey);
    }
    @Override
    public <T> List<T> getEntities(Class<T> entityClass){
        if(entityClass == null)
            return null;
        return dBase.getEntities(entityClass);
    }
    /**
     * Mevcut {@code HavsaleConfiguration} verisini diske yazar
     * @return İşlem sonucu
     */
    public boolean saveHavsaleConfiguration(){
        String json = this.getJSONWriter().produceText(null, getConfs());
        boolean isSuccessful = dBase.getIoServ().writeToFile(dBase.getPath(), CONF_FILE_NAME, json);
        String log = "HavsaleConfiguration yapılandırma dosyası kayıt işlemi " + (isSuccessful ? "BAŞARILI" : "BAŞARISIZ");
        writeLog(log);
        return isSuccessful;
    }
    // KORUNAN İŞLEM METOTLARI:
    /**
     * Verilen yapılandırmaya göre veriyi diskten sisteme yükler<br>
     * Bu işlem okuma, çeşitli dönüşümler, zerk işlemleri gibi işlemleri içerir
     * @param confs Diskten yükleme için gerekli sistem yapılandırması
     * @return İşlem tamâmen başarılıysa {@code true}, aksi hâlde {@code false}
     */
    protected boolean loadEntities(HavsaleConfiguration confs){
        boolean isSuccessful = true;
        if(confs != null){
            Set<Map.Entry<Class<?>, EntityConfiguration>> entries = 
                                        confs.getMapOfEntityToConfs().entrySet();
            Map<Class<?>, List<Map<String, Object>>> rawData = new HashMap<Class<?>, List<Map<String, Object>>>();
            for(Map.Entry<Class<?>, EntityConfiguration> entry : entries){
                List<String> entityTexts = dBase.getDiscRelationer().getEntityTexts(entry.getKey());
                List<Map<String, Object>> entitiesOfClass = new ArrayList<Map<String, Object>>();
                for(String t : entityTexts){
                    Map<String, Object> entity = JSONReader.getService().readJSONObject(t);
                    if(entity != null)
                        entitiesOfClass.add(entity);
                }
                rawData.put(entry.getKey(), entitiesOfClass);
            }
            try{
                getEntityRelationer().loadEntities(rawData, this.confs);
            }
            catch(Exception exc){
                System.err.println("Hatâ : " + exc.toString());
            }
            dBase.setEntities(this, getEntityRelationer().getProducedDB());
            String log = null;
            if(isSuccessful)
                log = "Sistem diskten BAŞARIYLA yüklendi.";
            else
                log = "Sistemin diskten yüklemesi BAŞARISIZ oldu.";
            writeLog(log);
            entityRelationer = null;
            if(isSuccessful)
                writeLog("Diskteki verinin yüklenmesi işlemi BAŞARILI");
            else
                writeLog("Diskteki işlemin yüklenmesi BAŞARISIZ");
        }
        return isSuccessful;
    }
    /**
     * Sistemin diskten yüklenmesini sağlayan tüm işlemleri başlatır<br>
     * Sistem yeni çalıştırılıyorsa, yapılandırma oluşturup, diske kaydeder
     * @return İşlem tamâmen başarılıysa {@code true}, aksi hâlde {@code false}
     */
    /*testte iken public yap*/ protected boolean loadSystem(){
        RWService ioServ = dBase.getIoServ();
        String confContent = ioServ.readDataAsText(dBase.getPath(), CONF_FILE_NAME);
        boolean isSuccessful = false;
        if(confContent != null){
            if(!confContent.isEmpty()){
                HavsaleConfiguration confs = JSONReader.getService()
                    .readJSONObjectReturnJSONObject(confContent)
                    .getThisObjectAsTargetType(HavsaleConfiguration.class,
                        jsoner.CODING_STYLE.CAMEL_CASE);
                if(confs != null){
                    isSuccessful = true;
                    this.confs = confs;// Mevcut yapılandırmayı, diskten okunanla değiştir
                }
            }
        }
        if(!isSuccessful){// Diskteki yapılandırma içe aktarılamadıysa;
            this.confs = HavsaleConfiguration.getDefaultOne();
            saveHavsaleConfiguration();// Mevcut yapılandırmayı diske yaz
        }
        else
            writeLog("HavsaleConfiguration yapılandırması içe aktarıldı.");
        return loadEntities(this.confs);
    }
    // ARKAPLAN İŞLEM METOTLARI:
    private void startingControl(String path) throws IllegalArgumentException{
        this.dBase = new DataB(path);// Dizin yolu hatâlı ise hatâ fırlatır
        writeLog("Sistem veritabanı başlatıldı.");
    }
    private EntityConfiguration getEntityConfiguration(Class<?> cls){
        EntityConfiguration entConfs = getConfs().getMapOfEntityToConfs().get(cls);
        if(entConfs == null){
            try{
                entConfs = new EntityConfiguration(cls);
                if(entConfs != null){
                    writeLog("Şu sınıf için yeni yapılandırma oluşturuldu : " + cls.getName());
                    getConfs().getMapOfEntityToConfs().put(cls, entConfs);
                    saveHavsaleConfiguration();
                }
            }
            catch(IllegalArgumentException exc){
                return null;
            }
        }
        return entConfs;
    }
    /**
     * Güvenlik kontrolleri yapıldıktan sonra nesne buraya verilmeli
     * @param entity Değeri alınmak istenen nesne
     * @param entConfs Sınıfın yapılandırması
     * @return Uygun biçimde dönüştürülmüş alan değerlerini içeren veri haritası
     */
    private Map<String, Object> prepareFieldValuesOfEntity(Object entity, EntityConfiguration entConfs){
        // Nesnenin ham verilerini al:
        Map<String, Object> pureValues = Reflector.getService()
                .getValueOfFields(entity, entConfs.getTakenFields(), getConfs().getCodingStyleInner(), true, true);
        // Test durumunda sıkça ihtiyaç duyulabilecek olan şu kod açılabilir:
//        System.out.println("pureValues:\n" + pureValues);
        // Kullanıcı tanımlı sınıf tipindeki alanların nesneleri yerine "id" değerini yerleştir:
        HavsaleRelationHandler.getService().
            replaceUserDefinedObjectWithId(pureValues,
                getConfs().getMapOfEntityToConfs(),getConfs().getCodingStyleInner());
        return pureValues;
    }
    private boolean addUpdateEntityMain(Object entity, boolean isAdding){
        EntityConfiguration entConfs = checkAndReturnEntityConfiguration(entity);
        if(entConfs == null)
            return false;
        
        // Verileri hâzırla:
        Map<String, Object> values = prepareFieldValuesOfEntity(entity, entConfs);
        
        // Hâzırlanan verileri JSON metnine çevir:
        String json = getJSONWriter().produceText(null, values);
        
        // Nesneyi ve metni kaydetmesi için DataB veritabanına gönder:
        boolean isSuccessful = false;
        Object id = values.get(entConfs.getNameOfIdField());
        if(isAdding)// Ekleme işlemi
            isSuccessful = dBase.addEntity(entity, id, json);
        else// Veri tazeleme (güncelleme) işlemi
            isSuccessful = dBase.updateEntity(entity, id, json);
        writeLog(entity.getClass().getName(), id, (isAdding ? 'a' : 'u'), isSuccessful);
        return isSuccessful;
    }
    private EntityConfiguration checkAndReturnEntityConfiguration(Object entity){
        if(entity == null)
            return null;
        Class<?> cls = entity.getClass();
        // 1) Yapılandırmayı al; yoksa oluştur; oluşturulamıyorsa 'sınıf uygun değil' hatâsı ver
        EntityConfiguration entConfs = getEntityConfiguration(cls);
        if(entConfs == null){
            writeLog("Verilen sınıf, nesne sınıfının özelliklerini taşımıyor : " + cls.getName());
            return null;
        }
        return entConfs;
    }
    /**
     * Verilen bilgilere göre kayıt (log) metni hâzırlar ve akışa yazar<br>
     * {@code process} parametresi şu değerleri alabilir:<br>
     * - u : update<br>
     * - a : add<br>
     * - d : delete<br>
     * @param entityClass İşlem gören nesnenin sınıfı
     * @param entityId İşlem gören nesnenin birincil anahtar değeri
     * @param process İşlem tipini simgeleyen harf
     * @param wasSuccessful İlgili işlem başarılı olduysa {@code true} verilmelidir
     */
    private void writeLog(String nameOfEntityClass, Object entityId, char process, boolean wasSuccessful){
        StringBuilder strBui = new StringBuilder("Veri ");
        String processText = "";
        switch(process){
            case 'a' : {processText = "ekleme "; break;}
            case 'u' : {processText = "tazeleme (güncelleme) "; break;}
            case 'd' : {processText = "silme "; break;}
        }
        strBui.append(processText).append("işlemi ").append((wasSuccessful ? "BAŞARILI" : "BAŞARISIZ"))
                .append("\t").append(nameOfEntityClass + "(id : " + entityId + ")");
        writeLog(strBui.toString());
    }
    private void writeLog(String message){
        writeLog(message, false);
    }
    private void writeLog(String message, boolean useDefaultCharset){
        if(message == null || streamLog == null || dBase == null)
            return;
        message = LocalDateTime.now() + "\t" + message + "\n";
        byte[] bytes = null;
        try{
            if(!useDefaultCharset)
                bytes = message.getBytes(Charset.forName("UTF-8"));
        }
        catch(UnsupportedCharsetException excOnCharset){
            useDefaultCharset = true;
        }
        if(useDefaultCharset)
            bytes = message.getBytes(Charset.defaultCharset());
            BufferedOutputStream bufOut = new BufferedOutputStream(streamLog);
        try{
            bufOut.write(bytes);
            bufOut.flush();
//            bufOut.close();// Bağlantı kapatılmamalı, belki sorun oluşturabilir
        }
        catch(Exception exc){
            System.err.println("Hedef akışa yazma yapılamıyor!.. : " + exc.toString());
        }
    }

// ERİŞİM METOTLARI:
    /**
     * Sistem kayıtlarının akıtılacağı akış nesnesini döndürür<br>
     * Bu, varsayılan olarak, aynı dizindeki "havsale.log" dosyasıdır
     * @return İlgili kayıt (log) akış nesnesi
     */
    public OutputStream getStreamOfLog(){
        return streamLog;
    }
    /**
     * @return Sistemin varsayılan yapılandırma dosya ismi
     */
    public static String getCONF_FILE_NAME(){
        return CONF_FILE_NAME;
    }
    /**
     * @return Sistemin varsayılan kayıt (log) dosya ismi
     */
    public static String getLOG_FILE_NAME(){
        return LOG_FILE_NAME;
    }
    // KORUNAN ERİŞİM METOTLARI:
    /**
     * Sistemin yapılandırma nesnesi döndürülür
     * @return Sistem yapılandırma nesnesi
     */
    /*testte iken public yap*/protected HavsaleConfiguration getConfs(){
        return confs;
    }
    /**
     * Sistemdeki veritabanı nesnesini döndürür
     * @return Sistem veritabanı nesnesi
     */
    protected DataB getdBase(){
        return dBase;
    }
    // GİZLİ ERİŞİM METOTLARI
    private JSONWriter getJSONWriter(){
        if(wrt == null)
            wrt = new JSONWriter();
        return wrt;
    }
    private EntityRelationer getEntityRelationer(){
        if(entityRelationer == null)
            entityRelationer = new EntityRelationer();
        return entityRelationer;
    }
}
