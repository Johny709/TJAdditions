package tj.capability;

public interface IEnderNotifiable<V> {

    void markToDirty();

    void setHandler(V handler);

    void setChannel(String channel);

    void setEntry(String entry);
}
