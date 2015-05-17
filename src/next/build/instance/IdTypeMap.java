package next.build.instance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IdTypeMap {

	private Map<String, Object> idMap;
	private Map<Class<?>, Object> typeMap;

	public IdTypeMap() {
		idMap = new ConcurrentHashMap<String, Object>();
		typeMap = new ConcurrentHashMap<Class<?>, Object>();
	}

	public Object get(Class<?> type, String id) {
		if ("".equals(id))
			return typeMap.get(type);
		return idMap.get(id);
	}

	public void put(String id, Class<?> type, Object obj) {
		if ("".equals(id)) {
			typeMap.put(type, obj);
			return;
		}
		idMap.put(id, obj);
	}

	@Override
	public String toString() {
		return "StringTypeMap [idMap=" + idMap + ", typeMap=" + typeMap + "]";
	}

}
