package pulse.math;

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
        if(!id.getClass().equals(ParameterIdentifier.class)) {
            return false;
        }
        
        var pid = (ParameterIdentifier) id;
        
        boolean result = true;
        
        if(keyword != pid.keyword || index != pid.index)
            result = false;
        
        return result;
        
    }
    
    @Override
    public String toString() {
        return keyword + " # " + index; 
    }
    
}