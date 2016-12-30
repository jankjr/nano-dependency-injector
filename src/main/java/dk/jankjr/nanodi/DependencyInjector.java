import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class DependencyInjector {
  /** Idea, recursively initialize fields in all dependencies.
   *
   * Store already initializes dependencies in the initializedDependencies map.
   *
   * Don't initialize a dependency of a particular type more than once.
   */
  private Map<Class, Object> initializedDependencies = new HashMap<>();
  public DependencyInjector(){
    initializedDependencies.put(DependencyInjector.class, this);
  }

  /**
   * fetches a getDependency based on the type. Perform any necessary DI if object
   * if all dependencies are not resolved.
   *
   * @param dependencyType
   * @param <T>
   * @return
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws NoSuchMethodException
   * @throws NoSuchFieldException
   * @throws InvocationTargetException
   */
  public<T> T getDependency(Class<T> dependencyType) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, NoSuchFieldException, InvocationTargetException {
    if (initializedDependencies.containsKey(dependencyType)) {
      return (T)initializedDependencies.get(dependencyType);
    }

    // Here are two cases to consider, either what we are initializing is an interface, in which case we need a
    // concrete type that fulfills the role of the interface. We get this by calling the getImpl method on the interface.
    T service;

    if(dependencyType.isInterface()){
      Method getImplMethod = dependencyType.getMethod("getImpl");
      if(getImplMethod == null){
        throw new RuntimeException("An interface must implement the getImpl() method to be an dependency");
      }

      Class<T> concreteType;
      concreteType = (Class<T>) getImplMethod.invoke(null);

      service = concreteType.newInstance();
    } else {
      service = dependencyType.newInstance();
    }

    // Save the dependency before initializing
    initializedDependencies.put(dependencyType, service);

    // Recursively inject dependencies
    injectDependencies(service);

    if(service instanceof Service){
      ((Service) service).init();
    }
    return service;
  }

  /**
   * Strategy, go over every field in object, check if it is annotated with a Dependency annotation,
   * if it is, perform DI on type.
   *
   * @param objectToInitialize
   * @param <T>
   * @return returns itself
   * @throws NoSuchFieldException
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws IllegalAccessException
   * @throws ClassNotFoundException
   * @throws InstantiationException
   */
  public<T> T injectDependencies(T objectToInitialize) throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, InstantiationException {
    for (Field f : objectToInitialize.getClass().getFields()) {
      for (Annotation a : f.getAnnotations()) {
        if (!a.annotationType().equals(Dependency.class)) { continue; }
        Object resolved = getDependency(f.getType());
        f.set(objectToInitialize, resolved);
      }
    }
    return (T)objectToInitialize;
  }
}