package havsale.Core;

import ReflectorRuntime.Reflector;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Veri nesnelerinin kullanıcı tanımlı alanlarını diske kaydedebilmek için
 * gerekli veri dönüşümünü yapar<br>
 * Kullanıcı tanımlı alanların değeri, referans verdiği nesnenin birincil
 * anahtarı yapılır; bu da ilişkileri verimli olarak diskte tutmayı sağlar
 * @author Mehmet Akif SOLAK
 */
public class HavsaleRelationHandler{
    private static HavsaleRelationHandler serv;

    public HavsaleRelationHandler(){}

// İŞLEM METOTLARI:
    // KORUNAN İŞLEM METOTLARI:
    /**
     * Verilen veriler uygun {@code String} formatına çevrilir.
     * @param pureValues Verilerin ham hâlleri
     * @return Verilerin serîleştirilmiş hâllerini içeren bir {@code Map} nesnesi
     */
    protected Map<String, Object> replaceUserDefinedObjectWithId(Map<String, Object> pureValues,
        Map<Class<?>, EntityConfiguration> confs, ReflectorRuntime.Reflector.CODING_STYLE codingStyle){
        Set<Map.Entry<String, Object>> entries = pureValues.entrySet();
        Map<String, Object> replaced = new HashMap<String, Object>();
        List<String> liExclude = new ArrayList<String>();
        for(Map.Entry<String, Object> entry : entries){
            String key = entry.getKey();// Alan ismi
            Object val = entry.getValue();// Alan değeri
            if(val == null)
                continue;
            if(!Reflector.getService().isNotUserDefinedClass(val.getClass())){
                Object idOfField = getIdValue(val, confs.get(val.getClass()), codingStyle);
                if(idOfField != null)
                    replaced.put(key, idOfField);
                else
                    liExclude.add(key);
            }
        }
        pureValues.putAll(replaced);
        
        // id değeri "null" olan, kullanıcı tanımlı sınıf tipindeki alanları kaydetme:
        for(String field : liExclude){
            pureValues.remove(field);
        }
        return pureValues;
    }
    /**
     * Verilen veri nesnesinin birincil anahtarının değerini döndürür<br>
     * Herhangi bir hatâ oluşursa veyâ nesne veri nesnesi değilse {@code null}
     * döndürülür
     * @param entity Veri nesnesi
     * @param entConfs Veri nesnesinin yapılandırması
     * @param codingStyle Alanların değerlerinin alınabilmesi için gerekli olan
     * kodlama biçimi parametresi
     * @return Birincil anahtar değeri veyâ {@code null}
     */
    protected static Object getIdValue(Object entity, EntityConfiguration entConfs,
        ReflectorRuntime.Reflector.CODING_STYLE codingStyle){
        if(entity == null)
            return null;
        String idField = (entConfs == null ? EntityConfiguration
            .getSuitableIdField(entity.getClass()) : entConfs.getNameOfIdField());
        if(idField == null)
            return null;
        List<String> vals = new ArrayList<String>();
        vals.add(idField);
        Map<String, Object> result = Reflector.getService()
            .getValueOfFields(entity, vals,
            codingStyle, true, true);
        try{
            return result.values().iterator().next();
        }
        catch(NullPointerException exc){
            return null;
        }
    }

// ERİŞİM METOTLARI:
    // ANA ERİŞİM METODU:
    /**
     * @return Bu yardımcı sınıfın "singleton" örneği
     */
    protected static HavsaleRelationHandler getService(){
        if(serv == null){
            serv = new HavsaleRelationHandler();
        }
        return serv;
    }
}