package havsale.Core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rwservice.RWService;

/**
 * Çalışma zamânı veritabanını barındırır ve idâre eder<br>
 * Diskteki veritabanındaki değişiklikleri yapmakla yükümlüdür<br>
 * VT işlemleri için metotlar sağlar; fakat bunlar {@code Systemer} tarafından
 * kullanılarak kullanıcıya sunulur
 * @author Mehmet Akif SOLAK
 */
public class DataB{
    private String path;
    private RWService ioServ;
    private Map<Class<?>, Map<Object, Object>> db;// Hafızadaki veritabanı,
            // yapısı : <Sınıf tam ismi, <birincil anahtar değeri, veri>>
    private HavsaleDiscRelationer discRelationer;

    /*protected yapılacak*/public DataB(String path) throws IllegalArgumentException{
        this.ioServ = new RWService();
        checkDirPath(path);
        this.path = path;
        this.db = new HashMap<Class<?>, Map<Object, Object>>();
        this.discRelationer = new HavsaleDiscRelationer(this);
    }

// İŞLEM METOTLARI:
    // KORUNAN İŞLEM METOTLARI:
    protected boolean addEntity(Object entity, Object primaryKeyValue, String jsonText){
        return addUpdateEntityMain(entity, primaryKeyValue, jsonText, true);
    }
    protected boolean updateEntity(Object entity, Object primaryKeyValue, String jsonText){
        return addUpdateEntityMain(entity, primaryKeyValue, jsonText, false);
    }
    protected boolean deleteEntity(Object entity, Object primaryKeyValue){
        if(entity == null || primaryKeyValue == null)
            return false;
        
        // 1) Veriyi diskten sil:
        boolean isSuccessful =  discRelationer.deleteFile(primaryKeyValue, entity.getClass().getName());
        
        // 2) Veriyi çalışma zamânı veritabanından sil:
        if(isSuccessful)
            getInitializedEntityMap(entity.getClass()).remove(primaryKeyValue);
        return isSuccessful;
    }
    protected <T> T getEntity(Class<T> entityClass, Object primaryKeyValue){
        try{
            return (T) getInitializedEntityMap(entityClass).get(primaryKeyValue);
        }
        catch(ClassCastException exc){
            return null;
        }
    }
    protected <T> List<T> getEntities(Class<T> entityClass){
        List<T> list = new ArrayList<T>();
        try{
            for(Object temp : getInitializedEntityMap(entityClass).values()){
                list.add((T) temp);
            }
            return list;
        }
        catch(ClassCastException exc){
            return null;
        }
    }
    protected void setEntities(Systemer manager, Map<Class<?>, Map<Object, Object>> db){
        if(manager == null)
            return;
        if(!manager.getdBase().equals(this))
            return;
        this.db = db;
    }
    // ARKAPLAN İŞLEM METOTLARI:
    private void checkDirPath(String path) throws IllegalArgumentException{
        String err = "Geçersiz dizin yolu tespit edildi.";
        if(path == null)
            throw new IllegalArgumentException(err);
        if(path.isEmpty())
            throw new IllegalArgumentException(err);
        try{
            File fl = new File(path);
            if(!fl.isDirectory())// Verilen dosya bir dizini işâret etmiyorsa;
                throw new IllegalArgumentException("Bir dizin yolu verilmeli");
//            System.err.println("checkIsReadable : " + ioServ.checkIsReadable(path) + "\tcheckIsWritable : "
//                    + ioServ.checkIsWritable(path) + "\tpath : " + path);
            if(!(ioServ.checkIsReadable(path) && ioServ.checkIsWritable(path)))// Okuma ve yazma izni yoksa;
                throw new IllegalArgumentException("Belirtilen dizin için okuma ve / veyâ yazma izni yok: " + fl.getAbsolutePath());
        }
        catch(SecurityException exc){
            throw new IllegalArgumentException("Verilen dizinin uygunluğu kontrol edilirken güvenlik hatâsı alındı");
        }
    }
    private Map<Object, Object> getEntityMap(Class<?> entityClass){
        return db.get(entityClass);
    }
    private Map<Object, Object> getInitializedEntityMap(Class<?> entityClass){
        Map<Object, Object> map = getEntityMap(entityClass);
        if(map == null){
            map = new HashMap<Object, Object>();
            db.put(entityClass, map);
        }
        return map;
    }
    private boolean addUpdateEntityMain(Object entity, Object primaryKeyValue, String jsonText, boolean isAdding){
        if(entity == null || primaryKeyValue == null)
            return false;
        
        // Verinin varlığını kontrol et:
        boolean isExist = isEntityExist(entity.getClass(), primaryKeyValue);
        if(isAdding){
            if(isExist){
                // log : Bu id alanına sâhip bir veri var; yeni veri eklenemedi!
                return false;
            }
        }
        else{
            if(!isExist){
                // log : Bu id alanına sâhip bir veri olmadığından veri tazelenemedi!
                return false;
            }
        }
        
        // 1) Veriyi çalışma zamânı veritabanına ekle:
        getInitializedEntityMap(entity.getClass()).put(primaryKeyValue, entity);
        
        // 2) Veriyi diskteki dosyaya yaz:
        boolean result = discRelationer.saveToFile(primaryKeyValue, entity.getClass().getName(), jsonText);
//        if(!result){
//            if(isAdding)
//                // log : Veri diske yazılamadığından veri eklenemedi
//            else
//                // log : Veri diske yazılamadığından diskteki veri değiştirilemedi
//        }
        return result;
    }
    private boolean isEntityExist(Class<?> entityClass, Object primaryKeyValue){
        if(entityClass == null || primaryKeyValue == null)
            return false;
        Map<Object, Object> mapOfThatClass = db.get(entityClass);
        if(mapOfThatClass == null)
            return false;
        return mapOfThatClass.get(primaryKeyValue) != null;
    }

// ERİŞİM METOTLARI:
    public String getPath(){
        return path;
    }
    // PAKET İÇİ ERİŞİM METOTLARI:
    protected RWService getIoServ(){
        return ioServ;
    }
    protected HavsaleDiscRelationer getDiscRelationer(){
        return discRelationer;
    }
}