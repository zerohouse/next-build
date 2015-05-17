package next.build.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 해당 필드의 인스턴스를 빌드합니다.<br>
 * 필드가 인터페이스일 경우 &#064;ImplementedBy 어노테이션을 사용해야 합니다.<br>
 * value를 지정하면 build.json을 읽어 Object화 합니다.<br>
 * <p>
 * ex)
 * 
 * <pre>
 * &#064;Build("users.root")
 * private User user
 * 
 * build.json
 * { 
 *  "users" :
 *     {
 *       "root" :
 *        {
 *         "email" : "next@begin.at",
 *         "id" : "next",
 *         "password" : "pass"
 *        }
 *     }
 *  }
 * </pre>
 * 
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Build {

	String id() default "";

	String source() default "";

	Class<?> ImplementedBy() default Object.class;

}
