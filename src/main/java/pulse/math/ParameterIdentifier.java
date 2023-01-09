package pulse.math;

import java.util.Objects;
import pulse.properties.NumericPropertyKeyword;

public class ParameterIdentifier {
 
    private NumericPropertyKeyword keyword;
    private int index;
    
    public ParameterIdentifier(NumericPropertyKeyword keyword, int index) {
        this.keyword = keyword;
        this.index = index;
    }
    
    public ParameterIdentifier(NumericPropertyKeyword keyword) {
        this(keyword, 0);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.keyword);
        hash = 29 * hash + this.index;
        return hash;
    }
    
    public ParameterIdentifier(int index) {
        this.index = index;
    }
    
    public NumericPropertyKeyword getKeyword() {
        return keyword;
    }
    
    public int getIndex() {
        return index;
    }
    
    @Override
    public boolean equals(Object id) {
        if(id.getClass() == null) {
            return false;
        }
        
        var classA = id.getClass();
        var classB = this.getClass();
        
        if(classA != classB) {
            return false;
        }
        
        var pid = (ParameterIdentifier) id;
        return keyword == pid.keyword && Math.abs(index - pid.index) < 1;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("").append(keyword);
        if(index > 0) {
            sb.append(" # ").append(index);
        }
        return sb.toString();
    }
    
}