package havsale.Core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Kullanıcı tanımlı sınıfflarda münferit kod ("id") alanını işâretlemek için
 * kullanılır<br>
 * Bu bildirim sınıf içerisinde yalnızca bir alanın tepesine eklenebilir
 * @author Mehmet Akif SOLAK
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdSpec{}
