package nextstep.study.di.stage4.annotations;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 스프링의 BeanFactory, ApplicationContext에 해당되는 클래스
 */
class DIContainer {

    private final Set<Object> beans;

    public DIContainer(final Set<Class<?>> classes) {
        this.beans = classes.stream()
                .map(this::instantiate)
                .collect(Collectors.toSet());
        beans.forEach(this::init);
    }

    private Object instantiate(final Class<?> clazz) {
        try {
            final Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void init(final Object instance) {
        for (final Field field : instance.getClass().getDeclaredFields()) {
            initDependency(instance, field);
        }
    }

    private void initDependency(final Object instance, final Field field) {
        final Class<?> type = field.getType();
        if (containsBeanBy(type) && field.isAnnotationPresent(Inject.class)) {
            setDependency(instance, field, getBean(type));
        }
    }

    private boolean containsBeanBy(final Class<?> type) {
        return beans.stream().anyMatch(type::isInstance);
    }

    private void setDependency(final Object instance, final Field field, final Object dependency) {
        try {
            field.setAccessible(true);
            field.set(instance, dependency);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static DIContainer createContainerForPackage(final String rootPackageName) {
        final Set<Class<?>> classes = ClassPathScanner.getAllClassesInPackage(rootPackageName);
        return new DIContainer(classes);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(final Class<T> aClass) {
        return (T) beans.stream()
                .filter(aClass::isInstance)
                .findFirst()
                .orElseThrow();
    }
}
