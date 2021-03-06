package com.fasterxml.jackson.databind.type;

import java.lang.reflect.*;
import java.util.*;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Helper class used for resolving type parameters for given class
 */
public class TypeBindings
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    private final static String[] NO_STRINGS = new String[0];

    private final static JavaType[] NO_TYPES = new JavaType[0];

    private final static TypeBindings EMPTY = new TypeBindings(NO_STRINGS, NO_TYPES, null);

    /**
     * Array of type (type variable) names.
     */
    private final String[] _names;

    /**
     * Types matching names
     */
    private final JavaType[] _types;

    /**
     * Names of potentially unresolved type variables.
     *
     * @since 2.3
     */
    private final String[] _unboundVariables;
    
    private final int _hashCode;
    
    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */
    
    private TypeBindings(String[] names, JavaType[] types, String[] uvars)
    {
        _names = (names == null) ? NO_STRINGS : names;
        _types = (types == null) ? NO_TYPES : types;
        if (_names.length != _types.length) {
            throw new IllegalArgumentException("Mismatching names ("+_names.length+"), types ("+_types.length+")");
        }
        int h = 1;
        for (int i = 0, len = _types.length; i < len; ++i) {
            h += _types[i].hashCode();
        }
        _unboundVariables = uvars;
        _hashCode = h;
    }

    public static TypeBindings emptyBindings() {
        return EMPTY;
    }

    // Let's just canonicalize serialized EMPTY back to static instance, if need be
    protected Object readResolve() {
        if ((_names == null) || (_names.length == 0)) {
            return EMPTY;
        }
        return this;
    }

    /**
     * Factory method for constructing bindings for given class using specified type
     * parameters.
     */
    public static TypeBindings create(Class<?> erasedType, List<JavaType> typeList)
    {
        JavaType[] types = (typeList == null || typeList.isEmpty()) ?
                NO_TYPES : typeList.toArray(new JavaType[typeList.size()]);
        return create(erasedType, types);
    }

    public static TypeBindings create(Class<?> erasedType, JavaType[] types)
    {
        if (types == null) {
            types = NO_TYPES;
        }
        TypeVariable<?>[] vars = erasedType.getTypeParameters();
        String[] names;
        if (vars == null || vars.length == 0) {
            names = NO_STRINGS;
        } else {
            int len = vars.length;
            names = new String[len];
            for (int i = 0; i < len; ++i) {
                names[i] = vars[i].getName();
            }
        }
        // Check here to give better error message
        if (names.length != types.length) {
            throw new IllegalArgumentException("Can not create TypeBindings for class "+erasedType.getName()
                   +" with "+types.length+" type parameter"
                   +((types.length == 1) ? "" : "s")+": class expects "+names.length);
        }
        return new TypeBindings(names, types, null);
    }

    public static TypeBindings create(Class<?> erasedType, JavaType typeArg1)
    {
        TypeVariable<?>[] vars = erasedType.getTypeParameters();
        int varLen = (vars == null) ? 0 : vars.length;
        if (varLen != 1) {
            throw new IllegalArgumentException("Can not create TypeBindings for class "+erasedType.getName()
                    +" with 1 type parameter: class expects "+varLen);
        }
        return new TypeBindings(new String[] { vars[0].getName() },
                new JavaType[] { typeArg1 }, null);
    }
    
    /**
     * Alternate factory method that may be called if it is possible that type
     * does or does not require type parameters; this is mostly useful for
     * collection- and map-like types.
     */
    public static TypeBindings createIfNeeded(Class<?> erasedType, JavaType typeArg1)
    {
        TypeVariable<?>[] vars = erasedType.getTypeParameters();
        int varLen = (vars == null) ? 0 : vars.length;
        if (varLen == 0) {
            return EMPTY;
        }
        if (varLen != 1) {
            throw new IllegalArgumentException("Can not create TypeBindings for class "+erasedType.getName()
                    +" with 1 type parameter: class expects "+varLen);
        }
        return new TypeBindings(new String[] { vars[0].getName() },
                new JavaType[] { typeArg1 }, null);
    }
    
    /**
     * Alternate factory method that may be called if it is possible that type
     * does or does not require type parameters; this is mostly useful for
     * collection- and map-like types.
     */
    public static TypeBindings createIfNeeded(Class<?> erasedType, JavaType[] types)
    {
        TypeVariable<?>[] vars = erasedType.getTypeParameters();
        if (vars == null || vars.length == 0) {
            return EMPTY;
        }
        if (types == null) {
            types = NO_TYPES;
        }
        int len = vars.length;
        String[] names = new String[len];
        for (int i = 0; i < len; ++i) {
            names[i] = vars[i].getName();
        }
        // Check here to give better error message
        if (names.length != types.length) {
            throw new IllegalArgumentException("Can not create TypeBindings for class "+erasedType.getName()
                   +" with "+types.length+" type parameter"
                   +((types.length == 1) ? "" : "s")+": class expects "+names.length);
        }
        return new TypeBindings(names, types, null);
    }
    
    /**
     * Method for creating an instance that has same bindings as this object,
     * plus an indicator for additional type variable that may be unbound within
     * this context; this is needed to resolve recursive self-references.
     * 
     * @since 1.3 (renamed from "withAdditionalBinding" in 1.2)
     */
    public TypeBindings withUnboundVariable(String name)
    {
        int len = (_unboundVariables == null) ? 0 : _unboundVariables.length;
        String[] names =  (len == 0)
                ? new String[1] : Arrays.copyOf(_unboundVariables, len+1);
        names[len] = name;
        return new TypeBindings(_names, _types, names);
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */
    
    /**
     * Find type bound to specified name, if there is one; returns bound type if so, null if not.
     */
    public JavaType findBoundType(String name)
    {
        for (int i = 0, len = _names.length; i < len; ++i) {
            if (name.equals(_names[i])) {
                return _types[i];
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return (_types.length == 0);
    }
    
    /**
     * Returns number of bindings contained
     */
    public int size() { 
        return _types.length;
    }

    public String getBoundName(int index)
    {
        if (index < 0 || index >= _names.length) {
            return null;
        }
        return _names[index];
    }

    public JavaType getBoundType(int index)
    {
        if (index < 0 || index >= _types.length) {
            return null;
        }
        return _types[index];
    }

    /**
     * Accessor for getting bound types in declaration order
     */
    public List<JavaType> getTypeParameters()
    {
        if (_types.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(_types);
    }

    /**
     * @since 2.3
     */
    public boolean hasUnbound(String name) {
        if (_unboundVariables != null) {
            for (int i = _unboundVariables.length; --i >= 0; ) {
                if (name.equals(_unboundVariables[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
    /**********************************************************************
    /* Standard methods
    /**********************************************************************
     */
    
    @Override public String toString()
    {
        if (_types.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('<');
        for (int i = 0, len = _types.length; i < len; ++i) {
            if (i > 0) {
                sb.append(',');
            }
//            sb = _types[i].appendBriefDescription(sb);
            String sig = _types[i].getGenericSignature();
            sb.append(sig);
        }
        sb.append('>');
        return sb.toString();
    }

    @Override public int hashCode() { return _hashCode; }

    @Override public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null || o.getClass() != getClass()) return false;
        TypeBindings other = (TypeBindings) o;
        int len = _types.length;
        if (len != other.size()) {
            return false;
        }
        JavaType[] otherTypes = other._types;
        for (int i = 0; i < len; ++i) {
            if (!otherTypes[i].equals(_types[i])) {
                return false;
            }
        }
        return true;
    }
    
    /*
    /**********************************************************************
    /* Package accessible methods
    /**********************************************************************
     */

    protected JavaType[] typeParameterArray() {
        return _types;
    }
}
