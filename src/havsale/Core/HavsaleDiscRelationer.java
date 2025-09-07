package havsale.Core;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import rwservice.RWService;
/**
 * Sınıf ve nesnelerin diskteki yerini saptamak için kullanılan yardımcıdır
 * @author Mehmet Akif SOLAK
 */
public class HavsaleDiscRelationer{
    private File root;
    private DataB dBase;
    private RWService ioServ;
    private char pathSeperator = System.getProperty("file.separator").charAt(0);

    protected HavsaleDiscRelationer(DataB dBase) throws IllegalArgumentException{
        if(dBase == null)
            throw new IllegalArgumentException("Verilen VT idârecisi 'null' olamaz");
        this.dBase = dBase;
        this.root = new File(dBase.getPath());
        this.ioServ = dBase.getIoServ();
    }

// İŞLEM METOTLARI:
    /**
     * Verilen dizindeki dosyaların içeriklerini okur ve döndürür<br>
     * @param cls Veri sınıfı
     * @return Veri sınıfı dizinindeki ".json" uzantılı dosya içerikleri
     */
    protected List<String> getEntityTexts(Class<?> cls){
        if(cls == null)
            return null;
        File dir = createDirsIfNotExistReturnFile(cls.getName());
        List<String> read = new ArrayList<String>();
        if(dir == null)
            return null;
        for(File fl : dir.listFiles(new FilenameFilter(){
            @Override
            public boolean accept(File dir, String name){
                if(dir.isDirectory()){
                    if(name.endsWith(".json"))
                        return true;
                }
                return false;
            }
        })){
            String data = ioServ.readDataAsText(fl);
            if(data != null){
                if(!data.isEmpty()){
                    read.add(data);
                }
            }
        }
        return read;
    }
    /**
     * Verilen veri sınıfı için diskte dizin üretir, bu sistemce belirtilen
     * yerleşime bağlı olarak yapılır<br>
     * Herhangi bir güvenlik hatâsı alınırsa {@code null} döndürülür
     * @param entityClassName Veri sınıfının tam ismi
     * @return Veri sınıfının dizini veyâ {@code null}
     */
    protected File createDirsIfNotExistReturnFile(String entityClassName){
        String path = entityClassName.replace('.', pathSeperator);
        try{
            File fl = new File(root, path);
            if(fl.exists())
                return fl;
            fl.mkdirs();
            return fl;
        }
        catch(SecurityException exc){}
        return null;
    }
    /**
     * Verilen bilgilere göre veri nesnesini hedef dosyaya yazar
     * @param id Veri nesnesinin birincil anahtar değeri
     * @param entityClassName Veri sınıfının tam ismi
     * @param content Dosyaya yazılmak istenen içerik
     * @return İşlem başarılıysa {@code true}, aksi hâlde {@code false}
     */
    protected boolean saveToFile(Object id, String entityClassName, String content){
        File lastDir = createDirsIfNotExistReturnFile(entityClassName);// İlgili dizinler yoksa oluştur
        if(lastDir == null)
            return false;
        String fileName = produceFileName(entityClassName, id);
        try{
            File target = new File(lastDir, fileName);
            if(!target.exists())
                target.createNewFile();
            return ioServ.writeToFile(target, content);
        }
        catch(IOException | SecurityException exc){
            return false;
        }
    }
    /**
     * Verilen bilgilerle belirtilen nesnenin diskteki dosya hâlini siler
     * @param id Nesne birincil anahtarı
     * @param entityClassName Veri sınıfının tam ismi
     * @return İşlem başarılıysa {@code true}, aksi hâlde {@code false}
     */
    protected boolean deleteFile(Object id, String entityClassName){
        File lastDir = createDirsIfNotExistReturnFile(entityClassName);
        String fileName = produceFileName(entityClassName, id);
        return ioServ.deleteFile(lastDir.getAbsolutePath(), fileName);
    }
    /**
     * Verilen veri sınıfı için dosya ismi üretir<br>
     * Dosya isimlendirmesi birincil anahtara göre yapılır
     * @param entityClassName Veri sınıfının tam ismi
     * @param id Verinin birincil anahtar değeri
     * @return Veri nesnesi için üretilen dosya ismi
     */
    protected String produceFileName(String entityClassName, Object id){
        if(entityClassName == null || id == null)
            return "";
        return String.valueOf(id) + ".json";
    }
// ERİŞİM METOTLARI:
}