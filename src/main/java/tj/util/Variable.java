package tj.util;

/**
 * Mainly used as a wrapper class for primitives to be treated as reference type.
 * @param <T> type
 */
public class Variable<T> {

    private T value;

    public Variable(T value) {
        this.value = value;
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Variable<?> ? ((Variable<?>) obj).getValue().equals(this.getValue()) : super.equals(obj);
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }
}
