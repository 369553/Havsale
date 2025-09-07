package havsale.Core;

import java.util.HashMap;
import java.util.Map;
/**
 * Sistemin tüm yapılandırmasını saklayan yapılandırma sınıfıdır
 * @author Mehmet Akif SOLAK
 */
public class HavsaleConfiguration{
    private CODING_STYLE codingStyle = CODING_STYLE.CAMEL_CASE;
    private Map<Class<?>, EntityConfiguration> mapOfEntityToConfs;// Hedef sınıfların yapılandırma verileri
//    private String dirPath;// Veritabanı olarak seçilen dizin

    public HavsaleConfiguration(){}

// İŞLEM METOTLARI:
    // STATİK ÜRETİM METODU:
    /**
     * Bir sistem yapılandırma örneğini varsayılan ayarlarla üretir
     * @return Varsayılan ayarlarla bir yapılandırma nesnesi döndürülür
     */
    public static HavsaleConfiguration getDefaultOne(){
        HavsaleConfiguration confs = new HavsaleConfiguration();
        return confs;
    }
    /**
     * Veri sınıflarının kodlama biçimini belirtmek için kullanılır
     * @param codingStyle Veri sınıflarının kodlama biçimi
     */
    public void setCodingStyle(CODING_STYLE codingStyle){
        this.codingStyle = codingStyle;
    }
//    public void setDirPath(String dirPath){
//        this.dirPath = dirPath;
//    }

// ERİŞİM METOTLARI:
    /**
     * Sistemdeki veri sınıflarının kodlama biçimini döndürür<br>
     * Varsayılan değer {@code havsale.Core.CODING_STYLE.CAMEL_CASE}'dir<br>
     * Eğer sınıflar bu biçimde yazılmadıysa, kullanıcı bunu değiştirmelidir
     * @return Veri sınıflarının kodlama biçimi
     */
    public CODING_STYLE getCodingStyle(){
        return codingStyle;
    }
    /**
     * Sistem içerisinde kullanılan, kodlama biçiminin {@code Reflector}
     * kütüphânesinin kodlama biçimi olarak döndürüldüğü bir metottur
     * @return Veri sınıflarının kodlama biçimi (Havsale sistemi için)
     */
    protected ReflectorRuntime.Reflector.CODING_STYLE getCodingStyleInner(){
        if(this.codingStyle == CODING_STYLE.CAMEL_CASE)
            return ReflectorRuntime.Reflector.CODING_STYLE.CAMEL_CASE;
        else if(this.codingStyle == CODING_STYLE.SNAKE_CASE)
            return ReflectorRuntime.Reflector.CODING_STYLE.SNAKE_CASE;
        return null;
    }
    /**
     * Veri sınıfları için yapılandırmaları barındıran harita döndürülür
     * @return Veri sınıfı yapılandırma haritası
     */
    public Map<Class<?>, EntityConfiguration> getMapOfEntityToConfs(){
        if(mapOfEntityToConfs == null)
            mapOfEntityToConfs = new HashMap<Class<?>, EntityConfiguration>();
        return mapOfEntityToConfs;
    }
}