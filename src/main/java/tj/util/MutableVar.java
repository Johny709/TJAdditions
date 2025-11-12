package tj.util;

public class MutableVar<T> {

    private T value;

    public MutableVar(T value) {
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
        return obj instanceof MutableVar<?> ? ((MutableVar<?>) obj).getValue().equals(this.getValue()) : super.equals(obj);
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
