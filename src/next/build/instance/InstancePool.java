package next.build.instance;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import next.build.annotation.Build;
import next.build.exception.TypeDuplicateException;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

public class InstancePool {

	private Map<Class<?>, Object> instanceMap;

	private BuildMap buildMap;

	private Set<Class<?>> methodLevel;
	private Set<Class<?>> classLevel;
	private Set<Class<?>> fieldLevel;

	private String basePackage;

	public InstancePool(String basePackage) throws TypeDuplicateException {
		methodLevel = new HashSet<Class<?>>();
		classLevel = new HashSet<Class<?>>();
		fieldLevel = new HashSet<Class<?>>();
		buildMap = new BuildMap(basePackage);
		this.basePackage = basePackage;
		instanceMap = new ConcurrentHashMap<Class<?>, Object>();
	}

	public Object getInstance(Class<?> type) {
		return instanceMap.get(type);
	}

	public Object getInstance(Method method) {
		return instanceMap.get(method.getDeclaringClass());
	}

	public Object getInstance(Field field) {
		return instanceMap.get(field.getDeclaringClass());
	}

	public void addMethodAnnotations(Class<?>... classes) {
		for (int i = 0; i < classes.length; i++) {
			methodLevel.add(classes[i]);
		}
	}

	public void addClassAnnotations(Class<?>... classes) {
		for (int i = 0; i < classes.length; i++) {
			classLevel.add(classes[i]);
		}
	}

	public void addFieldAnnotations(Class<?>... classes) {
		for (int i = 0; i < classes.length; i++) {
			fieldLevel.add(classes[i]);
		}
	}

	@SuppressWarnings("unchecked")
	public void build() {
		Reflections ref = new Reflections(basePackage, new SubTypesScanner(), new TypeAnnotationsScanner(), new FieldAnnotationsScanner(),
				new MethodAnnotationsScanner());
		Set<Class<?>> allTypes = new HashSet<Class<?>>();
		classLevel.forEach(annotation -> {
			allTypes.addAll(ref.getTypesAnnotatedWith((Class<? extends Annotation>) annotation));
		});
		methodLevel.forEach(annotation -> {
			ref.getMethodsAnnotatedWith((Class<? extends Annotation>) annotation).forEach(method -> {
				allTypes.add(method.getDeclaringClass());
			});
		});
		fieldLevel.forEach(annotation -> {
			ref.getFieldsAnnotatedWith((Class<? extends Annotation>) annotation).forEach(method -> {
				allTypes.add(method.getDeclaringClass());
			});
		});
		allTypes.forEach(type -> {
			Object obj = Parser.newInstance(type);
			buildFields(type, obj);
			instanceMap.put(type, obj);
		});
	}

	private void buildFields(Class<?> type, Object obj) {
		Field[] fields = type.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			if (!fields[i].isAnnotationPresent(Build.class))
				continue;
			setFields(obj, fields[i]);
		}
		Class<?> supperClass = type.getSuperclass();
		if (supperClass != null)
			buildFields(supperClass, obj);
	}

	private void setFields(Object obj, Field field) {
		field.setAccessible(true);
		try {
			field.set(obj, buildMap.get(field));
		} catch (Exception e) {
		}
	}

}
