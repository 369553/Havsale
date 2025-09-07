package havsale.Core;

/**
 * Kullanılan alt kütüphânelerde istenen kodlama biçimini ifâde etmek için
 * kullanılan bir {@code enum} sınıfıdır<br>
 * Bu kodlama biçimi 'getter' ve/veyâ 'setter' yöntemlerinin sınıf içerisinde
 * hangi isimle aranacağının bilinmesi için kullanılır<br>
 * Misal, {@code "number"} özelliğine doğrudan zerk yapılamazsa,<br>
 * {@code "CAMEL_CASE"} seçilirse, {@code "setNumber"}<br>
 * {@code "SNAKE_CASE"} seçilirse, {@code "set_number"}<br>
 * yöntemi kullanılarak ilgili değerin zerk edilmesi denenir.
 * @author Mehmet Akif SOLAK
 */
public enum CODING_STYLE{
        CAMEL_CASE,
        SNAKE_CASE
}
