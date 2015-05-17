package next.build.instance;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import next.build.annotation.Build;
import next.build.exception.TypeDuplicateException;
import next.build.jobject.JArray;
import next.build.jobject.JMap;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class BuildMap {

	private final static Logger logger = LoggerFactory.getLogger(BuildMap.class);

	private JMap jmap;
	private Set<Field> fields;
	private IdTypeMap idMap;
	private IdTypeMap sourceMap;

	public Object get(Class<?> type, String value) {
		return idMap.get(type, value);
	}

	public Object get(Field field) {
		return get(getType(field), getId(field));
	}

	public BuildMap(String basePackage) throws TypeDuplicateException {
		Reflections ref = new Reflections(basePackage, new FieldAnnotationsScanner());
		fields = ref.getFieldsAnnotatedWith(Build.class);
		idMap = new IdTypeMap();
		sourceMap = new IdTypeMap();
		idTypeCheck();
		JsonReader reader;
		try {
			reader = new JsonReader(new FileReader(getClass().getResource("/build.json").getFile()));
			jmap = new JMap(reader);

			fields.forEach(field -> {
				makeSourceMap(field);
			});

			fields.forEach(field -> {
				getIfNotExistBuild(getType(field), getId(field));
			});
			logger.debug(idMap.toString());

		} catch (IOException e) {
			logger.warn("세팅이 잘못되었습니다.");
			e.printStackTrace();
		}
	}

	private void makeSourceMap(Field field) {
		String source = getSource(field);
		if (!"".equals(source))
			sourceMap.put(getId(field), getType(field), getSource(field));
	}

	private void idTypeCheck() throws TypeDuplicateException {
		Map<String, Class<?>> typeMap = new HashMap<String, Class<?>>();
		Iterator<Field> iter = fields.iterator();
		while (iter.hasNext()) {
			Field field = iter.next();
			String id = getId(field);
			Class<?> type = typeMap.get(id);
			if ("".equals(id))
				continue;
			if (type == null) {
				typeMap.put(id, field.getType());
				continue;
			}
			if (!type.equals(field.getType()))
				throw new TypeDuplicateException(id, type, field.getType());
		}
	}

	private String getId(Field field) {
		return field.getAnnotation(Build.class).id();
	}

	private String getSource(Field field) {
		return field.getAnnotation(Build.class).source();
	}

	private Class<?> getType(Field field) {
		Class<?> type = field.getType();
		if (!field.getAnnotation(Build.class).ImplementedBy().equals(Object.class))
			type = field.getAnnotation(Build.class).ImplementedBy();
		return type;
	}

	private Object getIfNotExistBuild(Class<?> type, String id) {
		Object obj = idMap.get(type, id);
		if (obj != null)
			return obj;
		Object source = sourceMap.get(type, id);
		if (source == null)
			return buildEmpty(type, id);
		if ("".equals(source))
			return buildEmpty(type, id);
		if (jmap.get(source.toString()) == null)
			return buildEmpty(type, id);
		return buildJson(source.toString(), type, id);
	}

	private Object buildJson(String source, Class<?> type, String id) {
		Object obj = jmap.get(source);
		if (obj.getClass().equals(type)) {
			logger.debug(String.format("id:%s-> %s 빌드되었습니다.", id, type));
			idMap.put(id, type, obj);
			buildDependencies(obj);
			return obj;
		}
		if (obj.getClass().equals(JArray.class))
			return buildJArrayObj(type, id, (JArray) obj);
		if (obj.getClass().equals(JMap.class))
			return buildJMapObj(type, id, (JMap) obj);
		return buildEmpty(type, id);
	}

	private Object buildJMapObj(Class<?> type, String id, JMap jm) {
		Map<Object, Object> map = new HashMap<Object, Object>();
		Map<Object, Object> idmap = new HashMap<Object, Object>();
		jm.forEach((k, v) -> {
			if (v.toString().startsWith("#")) {
				idmap.put(k, v.toString().substring(1));
				return;
			}
			map.put(k, v);
		});
		Gson gson = new Gson(); // setting.getGson();
		Object obj = gson.fromJson(gson.toJson(map), type);
		this.idMap.put(id, type, obj);
		buildDependencies(obj);
		if (Map.class.isAssignableFrom(type)) {
			try {
				Method m = type.getMethod("put", Object.class, Object.class);
				idmap.forEach((k, v) -> {
					Object param = getIfNotExistBuild(type, v.toString());
					try {
						m.invoke(obj, k, param);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
			return obj;
		}
		try {
			idmap.forEach((k, v) -> {
				try {
					Field field = type.getDeclaredField(k.toString());
					Object param = getIfNotExistBuild(field.getType(), v.toString());
					field.setAccessible(true);
					field.set(obj, param);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj;
	}

	private Object buildJArrayObj(Class<?> type, String id, JArray jm) {
		List<Object> obList = new ArrayList<Object>();
		List<Object> idList = new ArrayList<Object>();
		jm.forEach(each -> {
			if (each.toString().startsWith("#")) {
				idList.add(each.toString().substring(1));
				return;
			}
			obList.add(each);
		});
		Gson gson = new Gson();// setting.getGson();
		Object obj = gson.fromJson(gson.toJson(obList), type);
		idMap.put(id, type, obj);
		buildDependencies(obj);
		if (List.class.isAssignableFrom(type)) {
			try {
				Method m = type.getMethod("add", Object.class);
				idList.forEach(v -> {
					Object param = getIfNotExistBuild(type, v.toString());
					try {
						m.invoke(obj, param);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		}
		return obj;
	}

	private Object buildEmpty(Class<?> type, String id) {
		Object obj = Parser.newInstance(type);
		idMap.put(id, type, obj);
		buildDependencies(obj);
		String log = id.equals("") ? "class:" + type : "id:" + id;
		logger.debug(String.format("%s -> %s 빌드되었습니다.", log, type));
		return obj;
	}

	private void buildDependencies(Object obj) {
		logger.debug(String.format("%s Dependency를 빌드합니다.", obj.getClass()));
		Field[] fields = obj.getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			if (!fields[i].isAnnotationPresent(Build.class))
				continue;
			fields[i].setAccessible(true);
			try {
				fields[i].set(obj, getIfNotExistBuild(getType(fields[i]), getId(fields[i])));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}

	}

	public String toString() {
		return "BuildMap [idmap=" + idMap + ",\n jmap=" + jmap + ",\n fields=" + fields + "]";
	}

}
