package linda;

import java.io.Serializable;
import java.util.Arrays;

public class Tuple implements Serializable {
    private final String[] fields;

    public Tuple(String[] fields) {
        if (fields == null) {
            throw new IllegalArgumentException("Tuple fields cannot be null");
        }
        this.fields = Arrays.copyOf(fields, fields.length);
    }

    public int length() {
        return fields.length;
    }

    public String get(int index) {
        return fields[index];
    }

    public String[] getFieldsCopy() {
        return Arrays.copyOf(fields, fields.length);
    }

    public boolean matches(String[] template) {
        if (template == null) return false;
        if (this.fields.length != template.length) return false;

        for (int i = 0; i < this.fields.length; i++) {
            if (template[i] != null && !template[i].equals(this.fields[i])) {
                return false;
            }
        }
        return true;
    }

    public void fillTemplate(String[] template) {
        if (template == null || this.fields.length != template.length) return;

        for (int i = 0; i < this.fields.length; i++) {
            template[i] = this.fields[i];
        }
    }

    @Override
    public String toString() {
        return Arrays.toString(fields);
    }
}
