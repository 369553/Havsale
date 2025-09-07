package havsale.Core;

import ReflectorRuntime.Reflector;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Bir veri sınıfını (entity) idâre etmek için gerekli yapılandırmaları saklar<br>
 * Bir sınıfın veri sınıfı olabilmesi şu şartları sağlaması gerekir:<br>
 * - Kullanıcı tanımlı olmalıdır<br>
 * - {@code id} isminde bir alanı veyâ {@code IdSpec} notasyonuyla işâretlenmiş
 * bir alanı olmalıdır<br>
 * Bu alan her nesne için farklı değer alan bir birincil anahtar olarak alınır<br>
 * Nesnelerin bu alanlarının değeri birbirinden farklı olmazsa veri kaybı olur
 * Eğer {@code IdSpec} bildirimi (notasyonu) kullanıdılysa o alan birincil
 * anahtar alanı olarak ele alınır<br>
 * - Alanlara erişebilmek için "getter" ve "setter" metotlarının olması
 * gerekebilir; bu, kullanılan {@code SecurityManager}'ın davranışına bağlı
 * olduğu için "getter" ve "setter" metotlarının olması gerekir
 * @author Mehmet Akif SOLAK
 */
public class EntityConfiguration{
    private Class<?> cls;
    private List<String> takenFields;
    private String nameOfIdField;

    /**
     * Verilen veri (entity) sınıfının yapılandırmasını oluşturur
     * @param cls Veri sınıfı
     * @throws IllegalArgumentException Verilen sınıf veri sınıfı değilse
     */
    public EntityConfiguration(Class<?> cls) throws IllegalArgumentException{
        this.cls = cls;
        checkClassOfEntity();
        extractConfs();
    }
    public EntityConfiguration(){}

// İŞLEM METOTLARI:
    /**
     * Verilen sınıfın uygun bir münferit kod ("id") alanı olup, olmadığı
     * kontrol edilir<br>
     * Eğer hiçbir uygun alan yoksa veyâ birden fazla {@code IdSpec} bildirimi 
     * olan alan varsa {@code null} döndürülür
     * @param cls Hedef sınıf
     * @return Uygun "id" alanının ismi veyâ {@code null}
     */
    protected static String getSuitableIdField(Class<?> cls){
        if(cls == null)
            return null;
        List<String> idSpectedFields = Reflector.getService()
                .getAnnotatedFieldsByGivenAnnotation(cls, IdSpec.class);
        boolean checkIdName = false;
        if(idSpectedFields == null)
            checkIdName = true;
        else if(idSpectedFields.isEmpty())
            checkIdName = true;
        if(checkIdName){
//            System.err.println("egrogefefe - "  + cls.getName());//yu
            if(!Reflector.getService().checkFieldIsExist(cls, "id"))
                return null;
            else
                return "id";
        }
        else if(idSpectedFields.size() == 1)
            return idSpectedFields.get(0);
        else
            return null;
    }
    @Override
    public String toString(){
        return "EntityConfiguration{" + "cls=" + cls + ", takenFields=" +
                takenFields + ", nameOfIdField=" + nameOfIdField + '}';
    }
// ARKAPLAN İŞLEM METOTLARI:
    private void checkClassOfEntity() throws IllegalArgumentException {
        String err = "Verilen sınıf kabûl edilmiyor; verilen sınıfın bir 'entity' sınıfı olmak için gerekenleri sağladığından emîn olunuz";
        if(cls == null)
            throw new IllegalArgumentException(err);
        if(Reflector.getService().isNotUserDefinedClass(cls))
            throw new IllegalArgumentException(err);
        String idField = getSuitableIdField(cls);
        if(idField == null)
            throw new IllegalArgumentException(err);
        this.nameOfIdField = idField;
    }
    private void extractConfs(){
        List<Field> liFields = Reflector.getService().getFields(cls, true);
        Field[] fields = new Field[liFields.size()];
        liFields.toArray(fields);
        for(Field fl : fields){
            Class<?> type = fl.getType();
            if(Reflector.getService().isNotUserDefinedClass(type)){// Değer olarak alınabilir veri tipi ise;
                getTakenFields().add(fl.getName());
            }
            else{// Kullanıcı tanımlı sınıf
                String suitableIdField = getSuitableIdField(type);
                if(suitableIdField == null)
                    continue;
                getTakenFields().add(fl.getName());
            }
        }
    }

// ERİŞİM METOTLARI:
    /**
     * Nesne sınıfının hangi alanlarının kaydedilmek üzere alındığı bilgisini
     * döndürür
     * @return Her veride alınması gereken alanların listesi
     */
    public List<String> getTakenFields(){
        if(takenFields == null)
            takenFields = new ArrayList<String>();
        return takenFields;
    }
    /**
     * Yapılandırmanın ilgili olduğu sınıfı döndürür
     * @return Bu yapılandırmayla ifâde edilen nesne sınıfı
     */
    public Class<?> getCls(){
        return cls;
    }
    /**
     * Yapılandırmanın ilgili olduğu sınıfı döndürür
     * @return Bu yapılandırmayla ifâde edilen nesne sınıfı
     */
    public Class<?> getClassOfEntity(){
        return cls;
    }
    /**
     * Nesne sınıfının hangi alanının birincil anahtar olduğu bilgisini döndürür
     * @return Birincil anahtar vazîfesi gören alanın ismi
     */
    public String getNameOfIdField(){
        return nameOfIdField;
    }
    /**
     * Bu metot "setter-injection" için bulunmaktadır
     * @param cls {@code cls} alanına zerk edilecek sınıf
     */
    public void setCls(Class<?> cls){
        this.cls = cls;
    }
    /**
     * Bu metot "setter-injection" için bulunmaktadır
     * @param takenFields {@code takenFields} alanına zerk edilecek veri
     */
    public void setTakenFields(List<String> takenFields){
        this.takenFields = takenFields;
    }
    /**
     * Bu metot "setter-injection" için bulunmaktadır
     * @param nameOfIdField {@code nameOfIdField} alanına zerk edilecek veri
     */
    public void setNameOfIdField(String nameOfIdField){    
        this.nameOfIdField = nameOfIdField;
    }
}