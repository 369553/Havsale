package havsale.Core;

import ReflectorRuntime.Reflector;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kullanıcı tanımlı nesneler arasındaki ilişkiyi göz önüne alarak nesneleri
 * oluşturmak ve zerk (injection) işlemlerini yapmakla görevlidir
 * @author Mehmet Akif SOLAK
 */
public class EntityRelationer{
    private Map<Class<?>, Map<Object, Object>> producedDB;

    public EntityRelationer(){}

// İŞLEM METOTLARI:
    // KORUNAN İŞLEM METOTLARI:
    /**
     * Verilen ham verilerden nesneleri üretir; şu işlemler yapılır:<br>
     * - Kullanıcı tanımlı sınıf tipli alanlar tespit edilir<br>
     * - Bu alanlar nesneler oluşturulduktan sonra birbirlerine zerk edilmek
     * üzere not edilir<br>
     * - Nesne oluşturma ve temel veri zerk (injection) işlemi yürütülür<br>
     * - Not edilen ilişkilere göre nesneler arasındaki ilişkiler nesnelere
     * zerk edilir<br>
     * @param rawData Ham verileri tutan harita nesnesi, şu biçimde olmalıdır:<br>
     * {nesne sınıfı : [{nesne alan ismi : nesne alan değeri}, ...]<br>
     * @return İşlem sırasında hiç hatâ olmadıysa {@code true}, değilse {@code false}
     */
    protected boolean loadEntities(Map<Class<?>, List<Map<String, Object>>> rawData, HavsaleConfiguration confs){
        if(rawData == null || confs == null)
            return false;
        ReflectorRuntime.Reflector.CODING_STYLE codingStyle = confs.getCodingStyleInner();
        boolean isSuccess = true;// İşlem tam başarılıysa true kalmalıdır
        Map<Class<?>, Map<Object, Object>> db = getProducedDB();// kısayol
        Map<Class<?>, Map<Object, Map<Field, Object>>> injectLater =
            new HashMap<Class<?>, Map<Object, Map<Field, Object>>>();// Sonra zerk edilecek alanları not et
        // Yapısı : <sınıf, <nesne, <alan ismi, o alana zerk edilecek nesnenin id'si>>>
        
        Map<Class<?>, List<Field>> dependencies = extractDependencies(rawData.keySet());
        
        Set<Map.Entry<Class<?>, List<Map<String, Object>>>> entries = rawData.entrySet();
        for(Map.Entry<Class<?>, List<Map<String, Object>>> entry : entries){// Tüm nesne sınıfları üzerinde gez
            Class<?> entityClass = entry.getKey();
            if(entityClass == null)
                continue;
            Map<Object, Object> entities = new HashMap<Object, Object>();// Üretilen nesneleri koymak için : <id, nesne>
            Map<Object, Map<Field, Object>> dependenciesOfEntities = new HashMap<Object, Map<Field, Object>>();
            // Yukarıdaki değişken üretilen nesnelerin sonra zerk edilecek bağımlılıklarını tutmak için.. <nesne, <alan, zerk edilecek nesne id'si>>
            // Nesneleri üret:
            for(Map<String, Object> data : entry.getValue()){// Nesne sınıfına âit tüm veriler üzerinde gez
                if(data == null)
                    continue;
                Object val = Reflector.getService().produceInjectedObject(entityClass, data, codingStyle);
                if(val != null){// Nesne oluşturulabildiyse;
                    Object id = data.get(confs.getMapOfEntityToConfs().get(entityClass).getNameOfIdField());
                    entities.put(id, val);
                    Map<Field, Object> dependenciesToInject = new HashMap<Field, Object>();// <Sonra zerk edilecek alan, dış anahtar değeri>
                    for(Field dependencyField : dependencies.get(entityClass)){// 
                        if(data.keySet().contains(dependencyField.getName())){// Bu alan için ilgili nesnenin bağımlılığı varsa;
                            Object foreignKey = data.get(dependencyField.getName());
                            if(foreignKey != null){
                                dependenciesToInject.put(dependencyField, foreignKey);
                            }
                        }
                    }
                    dependenciesOfEntities.put(val, dependenciesToInject);
                }
                else
                    isSuccess = false;
            }
            db.put(entityClass, entities);
            injectLater.put(entityClass, dependenciesOfEntities);
        }
        
        // inject dependencies:
       for(Map.Entry<Class<?>, Map<Object, Map<Field, Object>>> entry : injectLater.entrySet()){// Her sınıf için dolaş
           Class<?> entityClass = entry.getKey();
           if(entityClass == null)
               continue;
           for(Map.Entry<Object, Map<Field, Object>> objDepMap : entry.getValue().entrySet()){//Her nesne için dolaş
               Object obj = objDepMap.getKey();// Hedef nesne
               if(obj == null)
                   continue;
               Map<Field, Object> depMap = objDepMap.getValue();
               if(depMap == null)
                   continue;
               if(depMap.isEmpty())// Zerk edilecek hiçbir nesne bazlı alan yoksa;
                   continue;
               Map<String, Object> realValues = new HashMap<String, Object>();
               for(Map.Entry<Field, Object> fieldEntries : depMap.entrySet()){// Zerk edilmek üzere not alınan her alan üzerinde dolaş
                   Field field = fieldEntries.getKey();
                   Object foreignKey = fieldEntries.getValue();
                   Class<?> externalClass = field.getType();// Alanın tipi hâricî sınıfı belirtmektedir
                   Map<Object, Object> externalEntities = db.get(externalClass);
                   if(externalEntities != null){
                       Object targetValue = externalEntities.get(foreignKey);
                       if(targetValue != null)
                           realValues.put(field.getName(), targetValue);
                   }
               }
               if(!realValues.isEmpty())
               Reflector.getService().injectData(obj, realValues, codingStyle);
           }
       }
       return isSuccess;
    }
    // ARKAPLAN İŞLEM METOTLARI:
    /**
     * Verilen sınıfların alanları kontrol edilir; alanın tipi verilen listedeki
     * sınıflardan birisiyse o alanın ismi not edilir<br>
     * Geriye döndürülen veri formatı : {sınıf : [bağımlı alanların isimleri]}
     * @param entityClasses Kontrol edilmesi gereken sınıfları içeren koleksiyon
     * @return Sınıfların bağımlı alanlarının isimleri haritası
     */
    private Map<Class<?>, List<Field>> extractDependencies(Set<Class<?>> entityClasses){
        // Hangi sınıfta, hangi sınıfa referans verildiği bilgisin oluştur:
        Map<Class<?>, List<Field>> referToUserDefined = new HashMap<Class<?>, List<Field>>();
        for(Class<?> cls : entityClasses){
            List<Field> fields = Reflector.getService().getFields(cls, true);// Sınıfın tüm alanlarını al
            List<Field> userDefineds = new ArrayList<Field>();
            for(Field fl : fields){// Tüm alanlar üzerinde dolaş
                if(entityClasses.contains(fl.getType()))// Bu alan kullanıcı tanımlı bir sınıfa referans veren bir alan ise;
                    userDefineds.add(fl);
            }
            referToUserDefined.put(cls, userDefineds);
        }
        return referToUserDefined;
    }

// ERİŞİM METOTLARI:
    public Map<Class<?>, Map<Object, Object>> getProducedDB(){
        if(producedDB == null)
            producedDB = new HashMap<Class<?>, Map<Object, Object>>();
        return producedDB;
    }
}