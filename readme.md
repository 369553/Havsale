# Havsale (v1.0.1)

## GENEL BİLGİLER

- Bu, nesneler arasındaki ilişkiyi performanslı biçimde ele alan, veri nesnelerini ilişkilerle birlikte otomatik üreten, JSON formatlı, disk tabanlı bir nevî küçük bir veritabanı sistemidir.

- Havsale, mecazen "kavrama, anlayış kabiliyeti" anlamına gelmektedir.

- Veriler diskte, okunabilir metîn formatı olan JSON formatında saklanır.

- Nesneler kaydedilirken JSON veri tipine çevrilir, uygulama tarafında ise tekrar nesneye dönüştürülür.

- Nesneler arasındaki ilişkiler sistem tarafından birincil anahtarla ele alınarak kaydedilir. Bu, performansı ve uygulama içerisinde verinin münferiden (tekil olarak) bulunmasını sağlamak için çok güzel bir usûldür.

- Sistemin çalışması için ihtiyaç duyulan tek şey okuma - yazma erişim izni olan bir dizinin yoludur.

- Veriler kullanıcı tarafından girip okunabilir, kolaylıkla taşınabilir.

- Sistem kendi yaplandırmasını da bir dosyada saklar ve uygulama açıldığında bu yapılandırmaya göre verileri diskten geri yükler.

- Bu uygulama hakkında görüş bildirmek isterseniz `mehmedakifs@yaani.com` adresine e - posta atabilirsiniz.

- Yazar : Mehmet Akif SOLAK

## KURULUM

- Uygulamayı geliştirme yapmadan, olduğu gibi kullanmak istiyorsanız `dist` dizini altındaki `havsale.jar` dosyasını kullanabilirsiniz. Diğer türlü bu depoyu klonlayabilir ve derleyebilirsiniz.

## KULLANIM

#### Sistemi Başlatma

- Sistemi başlatmak için yeni bir `Systemer` nesnesi oluşturmalısınız.

- `Systemer`, sistemle iletişime geçtiğiniz tek ve yetkili sınıf.

- Örnek kod:
  
  ```java
  String path = "/home/akif/havsaleDB";
  Systemer sys = null;
  try{
      sys = new Systemer(path);
  }
  catch(IllegalArgumentException exc){
      System.err.println("Dizin adresini ve erişimleri kontrol edin");
  }
  ```

- Verilen dizin adresinin `null` olması, geçersiz olması veyâ gerekli izinleri taşımıyor olması durumunda `IllegalArgumentException` hatâsı fırlatılır.

- Sistem çalıştığında tüm veriler belleğe yüklenir. Veriler istendiğinde değil, en başta bellekten çekilir.

- Uygulamanın geliştirilme amacı küçük şahsî uygulamalar için veritabanından bağımsız ve okunabilir formatta veri tutulması ihtiyacını gidermek idi. Çalışma şekli de buna göre tasarlanmıştır.

#### Ne Tür Veriler Eklenebilir

- `havsale` kullanıcı tanımlı veri sınıflarınızın nesnelerini diskte tutmak için tasarlanmıştır.

- Bu, sistem üzerinden kaydetmek isteyeceğiniz nesnelerin veri sınıfı olmasını şart koşar

- Bir veri sınıfı, içerisinde her nesne için farklı değer alan bir alan taşıyan sınıftır. Buna veritabanındaki birincil anahtar `primary key` veyâ `id` diyebiliriz.

- Yanlış anlaşılma olmasın, geliştiricinin her nesne için o alanın değerinin farklı olmasını garanti altına alması gerekmiyor, sadece bu alanın birincil anahtar olduğunu sistemin tanıyabildiği bir formatta sunması gerekiyor; yanî nesnelerin `id` değerlerinin aynı olmaması yükü kullanıcıya da bırakılabilir.

- `havsale` bir sınıfın `id` ismindeki alanını otomatik olarak birincil anahtar kabûl eder; fakat eğer farklı isimdeki bir alanın birincil anahtar olmasını istiyorsanız o alanı `IdSpec` (`havsale.Core.IdSpec`) bildirimiyle (notasyonuyla) süslemeniz gerekmektedir:
  
  ```java
  // Aşağıdaki sınıfın id alanı birincil anahtar olarak alınır:
  public class User{
      int id;
      String name;
  }
  
  // Aşağıdaki sınıfın name alanı birincil anahtar olarak ele alınır:
  public class School{
      UUID id;
      @IdSpec
      String name;
  }
  
  // Aşağıdaki sınıfın birincil anahtarı olmadığı için bu sınıfın
  // nesneleri sisteme kaydedilemez:
  public class Person{
      String name;
      int age;
  }
  ```

- Sınıfın, her nesne için bu alanın münferit olmasını zorunlu kılacak bir yapıda olması gerekmiyor; geliştirici isterse bu alanın münferit değer alması yükünü kullanıcıya bırakabilir.Bu, sadece `havsale` sistemine ilgili alanın birincil anahtar gibi muamele görmesini anlatıyor.

> ***NOT :*** Birincil anahtar alanı için tip şartı yoktur; `int`, `String`, `UUID` gibi veri tiplerini kullanabilirsiniz.

- ..

#### Veri Ekleme

- Veri eklemek için `Systemer` nesnesi üzerinden `addEntity()` metodunu çalıştırmanız kâfîdir:
  
  ```java
  User u = new User(14, "Mehmet Akif");
  System.out.println("Veri eklendi mi : " + sys.addEntity(u));
  ```

- Sistem otomatik olarak sınıf alanlarını tarar, alınacak alanları, hedef sınıf ismini ve münferit değer (id) olarak kullanılan alanın ismini bir `EntityConfiguration` nesnesi olarak tutar. Eğer nesnenin sınıfı bir veri sınıfı değilse, işlem sonlandırılır ve kullanıcıya `false` döndürülür.

- Daha sonra veri hem bellek veritabanına, hem de diske yazılır.

#### Veri Tazeleme (Güncelleme)

- `updateEntity()` metoduyla veriyi yeniden yazabilirsiniz. Bunun için metoda verinin sınıfını ve tazelenmesi istenen veriyi vermeniz kâfîdir.

#### Veri Silme

- `deleteEntity()` metoduyla veriyi silebilirsiniz. Bunun için metoda silinmesini istediğiniz veriyi verebilirsiniz.

#### Veri Çekme - Sorgulama

- Sistem, bu işlemi iki metotla desteklemektedir. Birincisi bir sınıfa âit tüm verilerin çekilmesi, ikincisi ise bir sınıfın belirli bir nesnesinin birincil anahtar vasıtasıyla çekilmesi:
  
  ```java
  // Tüm nesnelerin çekilmesi:
  List<User> allUsers = sys.getEntities(User.class);
  
  // Belirli bir nesnenin çekilmesi (ikinci parametre birincil anahtar):
  User user = sys.getEntity(User.class, 13);
  ```

## DİĞER

- Sistem kayıtlarının tutulacağı için `Systemer` sınıfının `setStreamForLog()` ve `setStreamForLogFile()` metotlarını kullanabilirsiniz.

- Veri sınıflarının verilerinin zerk edilmesi veyâ alınması için getter - setter metotlarına ihtiyaç duyulabilir. Sistem varsayılan olarak bunu `havsale.core.CODING_STYLE.CAMEL_CASE` kodlama biçimine göre yapar. Eğer sınıflarınız bu kodlama biçiminde kodlanmamışsa bunu `` metoduyla değiştirebilirsiniz.

- 
