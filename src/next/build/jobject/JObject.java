package next.build.jobject;

public interface JObject {

	Object get(String string);

	JObject getNode(String string);

	Object get(String... keys);

}
