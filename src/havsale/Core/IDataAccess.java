package havsale.Core;

import java.util.List;

/**
 * Kullanıcının veriye erişimini sağlayan sınıfın uygulaması gereken arayüz
 * @author Mehmet Akif SOLAK
 */
public interface IDataAccess{

// VERİ EKLEME METODU:
    /**
     * Verilen veri nesnesinin sisteme ekler<br>
     * Bu işlem hem çalışma zamânı veritabanına, hem de diske yazmayı içerir
     * @param entity Veri nesnesi
     * @return İşlem başarılıysa {@code true}, aksi hâlde {@code false}
     */
    public boolean addEntity(Object entity);
// VERİ SİLME METODU:
    /**
     * Verilen veri nesnesini sistemden siler<br>
     * Bu işlem hem çalışma zamânı veritabanından, hem de diskten silmeyi içerir
     * @param entity Veri nesnesi
     * @return İşlem başarılıysa {@code true}, aksi hâlde {@code false}
     */
    public boolean deleteEntity(Object entity);
// VERİ TAZELEME METODU:
    /**
     * Verilen veri nesnesini tazeler (günceller)<br>
     * Bu işlem hem çalışma zamânı veritabanında, hem de diskte tazelemeyi içerir
     * @param entity Veri nesnesi
     * @return İşlem başarılıysa {@code true}, aksi hâlde {@code false}
     */
    public boolean updateEntity(Object entity);
// VERİLERİ ÇEKME METODU:
    /**
     * Verilen veri sınıfın kaydedilen tüm nesnelerini, yanî verileri getirir
     * @param <T> İlgili veri sınıfını ifâde eden tip
     * @param entityClass Veri sınıfı
     * @return İlgili verileri barındıran {@code List}
     */
    public <T> List<T> getEntities(Class<T> entityClass);
// MÜŞAHHAS VERİ ÇEKME METODU:
    /**
     * Verilen sınıftaki müşahhas veriyi (birincil anahtara göre) döndürür
     * @param <T> İlgili veri sınıfını ifâde eden tip
     * @param entityClass Veri sınıfı
     * @param valueOfPrimaryKey Aranan verinin birincil anahtar değeri
     * @return Bulunursa ilgili veri, bulunamazsa {@code null}
     */
    public <T> T getEntity(Class<T> entityClass, Object valueOfPrimaryKey);
}