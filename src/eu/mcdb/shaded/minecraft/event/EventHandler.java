package eu.mcdb.shaded.minecraft.event;

@FunctionalInterface
public interface EventHandler<T> {

    void handle(T object);

    default void handleSafe(T object) {
        try {
            handle(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
