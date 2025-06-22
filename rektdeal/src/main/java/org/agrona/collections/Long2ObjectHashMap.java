/*
 * Copyright 2014-2025 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agrona.collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;

import static java.util.Objects.requireNonNull;

// changes:
// removed DoNotSub annotation
// included static util functions validateLoadFactor, findNextPositivePowerOfTwo, and Hashing,hash
// removed some methods not used in this project

/**
 * {@link java.util.Map} implementation specialised for long keys using open addressing and
 * linear probing for cache efficient access.
 *
 * @param <V> type of values stored in the {@link java.util.Map}
 */
public class Long2ObjectHashMap<V> implements Map<Long, V>
{
    static void validateLoadFactor(final float loadFactor)
    {
        if (loadFactor < 0.1f || loadFactor > 0.9f)
        {
            throw new IllegalArgumentException("load factor must be in the range of 0.1 to 0.9: " + loadFactor);
        }
    }
    static int findNextPositivePowerOfTwo(final int value)
    {
        return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(value - 1));
    }
    static int hash(final long value)
    {
        long x = value;

        x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
        x = x ^ (x >>> 31);

        return (int)x ^ (int)(x >>> 32);
    }
    static int hash(final long value, final int mask)
    {
        return hash(value) & mask;
    }

    static final int MIN_CAPACITY = 8;

    private final float loadFactor;
    private int resizeThreshold;
    private int size;
    private final boolean shouldAvoidAllocation;

    private long[] keys;
    private Object[] values;

    private ValueCollection valueCollection;
    private KeySet keySet;
    private EntrySet entrySet;

    /**
     * Constructs map with given initial capacity and load factory and enables caching of iterators.
     *
     * @param initialCapacity for the backing array.
     * @param loadFactor      limit for resizing on puts.
     */
    public Long2ObjectHashMap(
        final int initialCapacity,
        final float loadFactor)
    {
        this(initialCapacity, loadFactor, true);
    }

    /**
     * Construct a new map allowing a configuration for initial capacity and load factor.
     *
     * @param initialCapacity       for the backing array.
     * @param loadFactor            limit for resizing on puts.
     * @param shouldAvoidAllocation should allocation be avoided by caching iterators and map entries.
     */
    public Long2ObjectHashMap(
        final int initialCapacity,
        final float loadFactor,
        final boolean shouldAvoidAllocation)
    {
        validateLoadFactor(loadFactor);

        this.loadFactor = loadFactor;
        this.shouldAvoidAllocation = shouldAvoidAllocation;

        /* */ final int capacity = findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, initialCapacity));
        /* */ resizeThreshold = (int)(capacity * loadFactor);

        keys = new long[capacity];
        values = new Object[capacity];
    }

    /**
     * Copy construct a new map from an existing one.
     *
     * @param mapToCopy for construction.
     */
    public Long2ObjectHashMap(final Long2ObjectHashMap<V> mapToCopy)
    {
        this.loadFactor = mapToCopy.loadFactor;
        this.resizeThreshold = mapToCopy.resizeThreshold;
        this.size = mapToCopy.size;
        this.shouldAvoidAllocation = mapToCopy.shouldAvoidAllocation;

        keys = mapToCopy.keys.clone();
        values = mapToCopy.values.clone();
    }

    /**
     * Get the load factor beyond which the map will increase size.
     *
     * @return load factor for when the map should increase size.
     */
    public float loadFactor()
    {
        return loadFactor;
    }

    /**
     * Get the total capacity for the map to which the load factor will be a fraction of.
     *
     * @return the total capacity for the map.
     */
    public int capacity()
    {
        return values.length;
    }

    /**
     * Get the actual threshold which when reached the map will resize.
     * This is a function of the current capacity and load factor.
     *
     * @return the threshold when the map will resize.
     */
    public int resizeThreshold()
    {
        return resizeThreshold;
    }

    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
    {
        return 0 == size;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(final Object key)
    {
        return containsKey((long)key);
    }

    /**
     * Overloaded version of {@link Map#containsKey(Object)} that takes a primitive long key.
     *
     * @param key for indexing the {@link Map}.
     * @return true if the key is found otherwise false.
     */
    public boolean containsKey(final long key)
    {
        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = hash(key, mask);

        boolean found = false;
        while (null != values[index])
        {
            if (key == keys[index])
            {
                found = true;
                break;
            }

            index = ++index & mask;
        }

        return found;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(final Object value)
    {
        boolean found = false;
        final Object val = mapNullValue(value);

        if (null != val)
        {
            final Object[] values = this.values;
            final int length = values.length;
            for (int i = 0, remaining = size; remaining > 0 && i < length; i++)
            {
                final Object existingValue = values[i];
                if (null != existingValue)
                {
                    if (Objects.equals(existingValue, val))
                    {
                        found = true;
                        break;
                    }
                    --remaining;
                }
            }
        }

        return found;
    }

    /**
     * {@inheritDoc}
     */
    public V get(final Object key)
    {
        return get((long)key);
    }

    /**
     * Overloaded version of {@link Map#get(Object)} that takes a primitive long key.
     *
     * @param key for indexing the {@link Map}.
     * @return the value if found otherwise null.
     */
    public V get(final long key)
    {
        return unmapNullValue(getMapped(key));
    }

    /**
     * Returns the value to which the specified key is mapped, or defaultValue if this map contains no mapping for the
     * key.
     *
     * @param key          whose associated value is to be returned.
     * @param defaultValue the default mapping of the key.
     * @return the value to which the specified key is mapped, or
     * {@code defaultValue} if this map contains no mapping for the key.
     */
    public V getOrDefault(final long key, final V defaultValue)
    {
        final V value = getMapped(key);
        return null != value ? unmapNullValue(value) : defaultValue;
    }

    /**
     * Get mapped value without boxing the key.
     *
     * @param key to get value by.
     * @return mapped value or {@code null}.
     */
    @SuppressWarnings("unchecked")
    protected V getMapped(final long key)
    {
        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = hash(key, mask);

        Object value;
        while (null != (value = values[index]))
        {
            if (key == keys[index])
            {
                break;
            }

            index = ++index & mask;
        }

        return (V)value;
    }

    /**
     * {@inheritDoc}
     */
    public V computeIfAbsent(final Long key, final Function<? super Long, ? extends V> mappingFunction)
    {
        return computeIfAbsent((long)key, mappingFunction::apply);
    }

    /**
     * Get a value for a given key, or if it does not exist then default the value
     * via a {@link java.util.function.LongFunction} and put it in the map.
     * <p>
     * Primitive specialized version of {@link Map#computeIfAbsent(Object, Function)}.
     *
     * @param key             to search on.
     * @param mappingFunction to provide a value if the get returns null.
     * @return the value if found otherwise the default.
     */
    public V computeIfAbsent(final long key, final LongFunction<? extends V> mappingFunction)
    {
        requireNonNull(mappingFunction);
        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = hash(key, mask);

        Object mappedValue;
        while (null != (mappedValue = values[index]))
        {
            if (key == keys[index])
            {
                break;
            }

            index = ++index & mask;
        }

        V value = unmapNullValue(mappedValue);

        if (null == value && (value = mappingFunction.apply(key)) != null)
        {
            values[index] = value;
            if (null == mappedValue)
            {
                keys[index] = key;
                if (++size > resizeThreshold)
                {
                    increaseCapacity();
                }
            }
        }

        return value;
    }

    /**
     * {@inheritDoc}
     */
    public V computeIfPresent(
        final Long key, final BiFunction<? super Long, ? super V, ? extends V> remappingFunction)
    {
        return computeIfPresent((long)key, remappingFunction::apply);
    }

    /**
     * {@inheritDoc}
     */
    public V compute(final Long key, final BiFunction<? super Long, ? super V, ? extends V> remappingFunction)
    {
        return compute((long)key, remappingFunction::apply);
    }

    /**
     * {@inheritDoc}
     */
    public V merge(
        final Long key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction)
    {
        return merge((long)key, value, remappingFunction);
    }

    /**
     * Primitive specialised version of {@link Map#merge(Object, Object, BiFunction)}.
     *
     * @param key               with which the resulting value is to be associated.
     * @param value             the non-null value to be merged with the existing value
     *                          associated with the key or, if no existing value or a null value
     *                          is associated with the key, to be associated with the key.
     * @param remappingFunction the function to recompute a value if present.
     * @return the new value associated with the specified key, or null if no
     * value is associated with the key.
     */
    public V merge(final long key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction)
    {
        requireNonNull(value);
        requireNonNull(remappingFunction);
        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = hash(key, mask);

        Object mappedvalue;
        while (null != (mappedvalue = values[index]))
        {
            if (key == keys[index])
            {
                break;
            }

            index = ++index & mask;
        }

        final V oldValue = unmapNullValue(mappedvalue);
        final V newValue = null == oldValue ? value : remappingFunction.apply(oldValue, value);

        if (null != newValue)
        {
            values[index] = newValue;
            if (null == mappedvalue)
            {
                keys[index] = key;
                if (++size > resizeThreshold)
                {
                    increaseCapacity();
                }
            }
        }
        else if (null != mappedvalue)
        {
            values[index] = null;
            size--;
            compactChain(index);
        }

        return newValue;
    }

    /**
     * {@inheritDoc}
     */
    public V put(final Long key, final V value)
    {
        return put((long)key, value);
    }

    /**
     * Overloaded version of {@link Map#put(Object, Object)} that takes a primitive long key.
     *
     * @param key   for indexing the {@link Map}.
     * @param value to be inserted in the {@link Map}.
     * @return the previous value if found otherwise null.
     */
    @SuppressWarnings("unchecked")
    public V put(final long key, final V value)
    {
        final V val = (V)mapNullValue(value);
        requireNonNull(val, "value cannot be null");

        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = hash(key, mask);

        Object oldValue;
        while (null != (oldValue = values[index]))
        {
            if (key == keys[index])
            {
                break;
            }

            index = ++index & mask;
        }

        if (null == oldValue)
        {
            ++size;
            keys[index] = key;
        }

        values[index] = val;

        if (size > resizeThreshold)
        {
            increaseCapacity();
        }

        return unmapNullValue(oldValue);
    }

    /**
     * {@inheritDoc}
     */
    public V remove(final Object key)
    {
        return remove((long)key);
    }

    /**
     * Overloaded version of {@link Map#remove(Object)} that takes a primitive long key.
     *
     * @param key for indexing the {@link Map}.
     * @return the value if found otherwise null.
     */
    public V remove(final long key)
    {
        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = hash(key, mask);

        Object value;
        while (null != (value = values[index]))
        {
            if (key == keys[index])
            {
                values[index] = null;
                --size;

                compactChain(index);
                break;
            }

            index = ++index & mask;
        }

        return unmapNullValue(value);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public boolean remove(final Object key, final Object value)
    {
        return remove((long)key, (V)value);
    }

    /**
     * Primitive specialised version of {@link Map#remove(Object, Object)}.
     *
     * @param key   with which the specified value is associated.
     * @param value expected to be associated with the specified key.
     * @return {@code true} if the value was removed.
     */
    public boolean remove(final long key, final V value)
    {
        final Object val = mapNullValue(value);
        if (null != val)
        {
            final long[] keys = this.keys;
            final Object[] values = this.values;
            final int mask = values.length - 1;
            int index = hash(key, mask);

            Object mappedValue;
            while (null != (mappedValue = values[index]))
            {
                if (key == keys[index])
                {
                    if (Objects.equals(unmapNullValue(mappedValue), value))
                    {
                        values[index] = null;
                        --size;

                        compactChain(index);
                        return true;
                    }
                    break;
                }

                index = ++index & mask;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        if (size > 0)
        {
            Arrays.fill(values, null);
            size = 0;
        }
    }

    /**
     * Compact the {@link Map} backing arrays by rehashing with a capacity just larger than current size
     * and giving consideration to the load factor.
     */
    public void compact()
    {
        final int idealCapacity = (int)Math.round(size() * (1.0d / loadFactor));
        rehash(findNextPositivePowerOfTwo(Math.max(MIN_CAPACITY, idealCapacity)));
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(final Map<? extends Long, ? extends V> map)
    {
        for (final Entry<? extends Long, ? extends V> entry : map.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Put all values from the given map longo this one without allocation.
     *
     * @param map whose value are to be added.
     */
    public void putAll(final Long2ObjectHashMap<? extends V> map)
    {
        final Long2ObjectHashMap<? extends V>.EntryIterator iterator = map.entrySet().iterator();
        while (iterator.hasNext())
        {
            iterator.findNext();
            put(iterator.getLongKey(), iterator.getValue());
        }
    }

    /**
     * Primitive specialised version of {@link #putIfAbsent(Object, Object)}.
     *
     * @param key   with which the specified value is to be associated.
     * @param value to be associated with the specified key.
     * @return the previous value associated with the specified key, or
     * {@code null} if there was no mapping for the key.
     */
    @SuppressWarnings("unchecked")
    public V putIfAbsent(final long key, final V value)
    {
        final V val = (V)mapNullValue(value);
        requireNonNull(val, "value cannot be null");

        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = hash(key, mask);

        Object mappedValue;
        while (null != (mappedValue = values[index]))
        {
            if (key == keys[index])
            {
                break;
            }

            index = ++index & mask;
        }

        final V oldValue = unmapNullValue(mappedValue);
        if (null == oldValue)
        {
            if (null == mappedValue)
            {
                ++size;
                keys[index] = key;
            }

            values[index] = val;

            if (size > resizeThreshold)
            {
                increaseCapacity();
            }
        }

        return oldValue;
    }

    /**
     * {@inheritDoc}
     */
    public KeySet keySet()
    {
        if (null == keySet)
        {
            keySet = new KeySet();
        }

        return keySet;
    }

    /**
     * {@inheritDoc}
     */
    public ValueCollection values()
    {
        if (null == valueCollection)
        {
            valueCollection = new ValueCollection();
        }

        return valueCollection;
    }

    /**
     * {@inheritDoc}
     */
    public EntrySet entrySet()
    {
        if (null == entrySet)
        {
            entrySet = new EntrySet();
        }

        return entrySet;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        if (isEmpty())
        {
            return "{}";
        }

        final EntryIterator entryIterator = new EntryIterator();
        entryIterator.reset();

        final StringBuilder sb = new StringBuilder().append('{');
        while (true)
        {
            entryIterator.next();
            sb.append(entryIterator.getLongKey()).append('=').append(unmapNullValue(entryIterator.getValue()));
            if (!entryIterator.hasNext())
            {
                return sb.append('}').toString();
            }
            sb.append(',').append(' ');
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof Map))
        {
            return false;
        }

        final Map<?, ?> that = (Map<?, ?>)o;

        if (size != that.size())
        {
            return false;
        }

        final long[] keys = this.keys;
        final Object[] values = this.values;
        for (int i = 0, length = values.length; i < length; i++)
        {
            final Object thisValue = values[i];
            if (null != thisValue)
            {
                final Object thatValue = that.get(keys[i]);
                if (!thisValue.equals(mapNullValue(thatValue)))
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        int result = 0;

        final long[] keys = this.keys;
        final Object[] values = this.values;
        for (int i = 0, length = values.length; i < length; i++)
        {
            final Object value = values[i];
            if (null != value)
            {
                result += (Long.hashCode(keys[i]) ^ value.hashCode());
            }
        }

        return result;
    }

    /**
     * Longerceptor for masking null values.
     *
     * @param value value to mask.
     * @return masked value.
     */
    protected Object mapNullValue(final Object value)
    {
        return value;
    }

    /**
     * Longerceptor for unmasking null values.
     *
     * @param value value to unmask.
     * @return unmasked value.
     */
    @SuppressWarnings("unchecked")
    protected V unmapNullValue(final Object value)
    {
        return (V)value;
    }

    /**
     * Primitive specialised version of {@link Map#replace(Object, Object)}.
     *
     * @param key   key with which the specified value is associated.
     * @param value value to be associated with the specified key.
     * @return the previous value associated with the specified key, or
     * {@code null} if there was no mapping for the key.
     */
    @SuppressWarnings("unchecked")
    public V replace(final long key, final V value)
    {
        final V val = (V)mapNullValue(value);
        requireNonNull(val, "value cannot be null");

        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = hash(key, mask);

        Object oldValue;
        while (null != (oldValue = values[index]))
        {
            if (key == keys[index])
            {
                values[index] = val;
                break;
            }

            index = ++index & mask;
        }

        return unmapNullValue(oldValue);
    }

    /**
     * Primitive specialised version of {@link Map#replace(Object, Object, Object)}.
     *
     * @param key      key with which the specified value is associated.
     * @param oldValue value expected to be associated with the specified key.
     * @param newValue value to be associated with the specified key.
     * @return {@code true} if the value was replaced.
     */
    @SuppressWarnings("unchecked")
    public boolean replace(final long key, final V oldValue, final V newValue)
    {
        final V val = (V)mapNullValue(newValue);
        requireNonNull(val, "value cannot be null");

        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = hash(key, mask);

        Object mappedValue;
        while (null != (mappedValue = values[index]))
        {
            if (key == keys[index])
            {
                if (Objects.equals(unmapNullValue(mappedValue), oldValue))
                {
                    values[index] = val;
                    return true;
                }
                break;
            }

            index = ++index & mask;
        }

        return false;
    }

    private void increaseCapacity()
    {
        final int newCapacity = values.length << 1;
        if (newCapacity < 0)
        {
            throw new IllegalStateException("max capacity reached at size=" + size);
        }

        rehash(newCapacity);
    }

    private void rehash(final int newCapacity)
    {
        final int mask = newCapacity - 1;
        /* */ resizeThreshold = (int)(newCapacity * loadFactor);

        final long[] tempKeys = new long[newCapacity];
        final Object[] tempValues = new Object[newCapacity];

        final long[] keys = this.keys;
        final Object[] values = this.values;
        for (int i = 0, size = values.length; i < size; i++)
        {
            final Object value = values[i];
            if (null != value)
            {
                final long key = keys[i];
                int index = hash(key, mask);
                while (null != tempValues[index])
                {
                    index = ++index & mask;
                }

                tempKeys[index] = key;
                tempValues[index] = value;
            }
        }

        this.keys = tempKeys;
        this.values = tempValues;
    }

    @SuppressWarnings("FinalParameters")
    private void compactChain(int deleteIndex)
    {
        final long[] keys = this.keys;
        final Object[] values = this.values;
        final int mask = values.length - 1;
        int index = deleteIndex;
        while (true)
        {
            index = ++index & mask;
            final Object value = values[index];
            if (null == value)
            {
                break;
            }

            final long key = keys[index];
            final int hash = hash(key, mask);

            if ((index < hash && (hash <= deleteIndex || deleteIndex <= index)) ||
                (hash <= deleteIndex && deleteIndex <= index))
            {
                keys[deleteIndex] = key;
                values[deleteIndex] = value;

                values[index] = null;
                deleteIndex = index;
            }
        }
    }

    /**
     * Set of keys which supports optionally cached iterators to avoid allocation.
     */
    public final class KeySet extends AbstractSet<Long>
    {
        private final KeyIterator keyIterator = shouldAvoidAllocation ? new KeyIterator() : null;

        /**
         * Create a new instance.
         */
        public KeySet()
        {
        }

        /**
         * {@inheritDoc}
         */
        public KeyIterator iterator()
        {
            KeyIterator keyIterator = this.keyIterator;
            if (null == keyIterator)
            {
                keyIterator = new KeyIterator();
            }

            keyIterator.reset();
            return keyIterator;
        }

        /**
         * {@inheritDoc}
         */
        public int size()
        {
            return Long2ObjectHashMap.this.size();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(final Object o)
        {
            return Long2ObjectHashMap.this.containsKey(o);
        }

        /**
         * Checks if the key is contained in the map.
         *
         * @param key to check.
         * @return {@code true} if the key is contained in the map.
         */
        public boolean contains(final long key)
        {
            return Long2ObjectHashMap.this.containsKey(key);
        }

        /**
         * {@inheritDoc}
         */
        public boolean remove(final Object o)
        {
            return null != Long2ObjectHashMap.this.remove(o);
        }

        /**
         * Removes key and the corresponding value from the map.
         *
         * @param key to be removed.
         * @return {@code true} if the mapping was removed.
         */
        public boolean remove(final long key)
        {
            return null != Long2ObjectHashMap.this.remove(key);
        }

        /**
         * {@inheritDoc}
         */
        public void clear()
        {
            Long2ObjectHashMap.this.clear();
        }

        /**
         * Removes all the elements of this collection that satisfy the given predicate.
         * <p>
         * NB: Renamed from removeIf to avoid overloading on parameter types of lambda
         * expression, which doesn't play well with type inference in lambda expressions.
         *
         * @param filter a predicate to apply.
         * @return {@code true} if at least one key was removed.
         */
        public boolean removeIfLong(final LongPredicate filter)
        {
            boolean removed = false;
            final KeyIterator iterator = iterator();
            while (iterator.hasNext())
            {
                if (filter.test(iterator.nextLong()))
                {
                    iterator.remove();
                    removed = true;
                }
            }
            return removed;
        }
    }

    /**
     * Collection of values which supports optionally cached iterators to avoid allocation.
     */
    public final class ValueCollection extends AbstractCollection<V>
    {
        private final ValueIterator valueIterator = shouldAvoidAllocation ? new ValueIterator() : null;

        /**
         * Create a new instance.
         */
        public ValueCollection()
        {
        }

        /**
         * {@inheritDoc}
         */
        public ValueIterator iterator()
        {
            ValueIterator valueIterator = this.valueIterator;
            if (null == valueIterator)
            {
                valueIterator = new ValueIterator();
            }

            valueIterator.reset();
            return valueIterator;
        }

        /**
         * {@inheritDoc}
         */
        public int size()
        {
            return Long2ObjectHashMap.this.size();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(final Object o)
        {
            return Long2ObjectHashMap.this.containsValue(o);
        }

        /**
         * {@inheritDoc}
         */
        public void clear()
        {
            Long2ObjectHashMap.this.clear();
        }

        /**
         * {@inheritDoc}
         */
        public void forEach(final Consumer<? super V> action)
        {
            int remaining =
                Long2ObjectHashMap.this.size;

            final Object[] values = Long2ObjectHashMap.this.values;
            for (int i = 0, length = values.length; remaining > 0 && i < length; i++)
            {
                final Object value = values[i];
                if (null != value)
                {
                    action.accept(unmapNullValue(value));
                    --remaining;
                }
            }
        }
    }

    /**
     * Set of entries which supports access via an optionally cached iterator to avoid allocation.
     */
    public final class EntrySet extends AbstractSet<Map.Entry<Long, V>>
    {
        private final EntryIterator entryIterator = shouldAvoidAllocation ? new EntryIterator() : null;

        /**
         * Create a new instance.
         */
        public EntrySet()
        {
        }

        /**
         * {@inheritDoc}
         */
        public EntryIterator iterator()
        {
            EntryIterator entryIterator = this.entryIterator;
            if (null == entryIterator)
            {
                entryIterator = new EntryIterator();
            }

            entryIterator.reset();
            return entryIterator;
        }

        /**
         * {@inheritDoc}
         */
        public int size()
        {
            return Long2ObjectHashMap.this.size();
        }

        /**
         * {@inheritDoc}
         */
        public void clear()
        {
            Long2ObjectHashMap.this.clear();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(final Object o)
        {
            if (!(o instanceof Entry))
            {
                return false;
            }

            final Entry<?, ?> entry = (Entry<?, ?>)o;
            final long key = (Long)entry.getKey();
            final V value = getMapped(key);
            return null != value && value.equals(mapNullValue(entry.getValue()));
        }

        /**
         * {@inheritDoc}
         */
        public Object[] toArray()
        {
            return toArray(new Object[size()]);
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(final T[] a)
        {
            final T[] array = a.length >= size ?
                a : (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
            final EntryIterator it = iterator();

            for (int i = 0; i < array.length; i++)
            {
                if (it.hasNext())
                {
                    it.next();
                    array[i] = (T)it.allocateDuplicateEntry();
                }
                else
                {
                    array[i] = null;
                    break;
                }
            }

            return array;
        }
    }

    /**
     * Base iterator implementation that contains basic logic of traversing the element in the backing array.
     *
     * @param <T> type of elements.
     */
    abstract class AbstractIterator<T> implements Iterator<T>
    {
        private int posCounter;
        private int stopCounter;
        private int remaining;
        boolean isPositionValid = false;

        /**
         * Position of the current element.
         *
         * @return position of the current element.
         */
        protected final int position()
        {
            return posCounter & (values.length - 1);
        }

        /**
         * Number of remaining elements.
         *
         * @return number of remaining elements.
         */
        public int remaining()
        {
            return remaining;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext()
        {
            return remaining > 0;
        }

        /**
         * Find the next element.
         *
         * @throws NoSuchElementException if no more elements.
         */
        protected final void findNext()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }

            final Object[] values = Long2ObjectHashMap.this.values;
            final int mask = values.length - 1;

            for (int i = posCounter - 1, stop = stopCounter; i >= stop; i--)
            {
                final int index = i & mask;
                if (null != values[index])
                {
                    posCounter = i;
                    isPositionValid = true;
                    --remaining;
                    return;
                }
            }

            isPositionValid = false;
            throw new IllegalStateException();
        }

        /**
         * {@inheritDoc}
         */
        public abstract T next();

        /**
         * {@inheritDoc}
         */
        public void remove()
        {
            if (isPositionValid)
            {
                final int position = position();
                values[position] = null;
                --size;

                compactChain(position);

                isPositionValid = false;
            }
            else
            {
                throw new IllegalStateException();
            }
        }

        final void reset()
        {
            remaining = Long2ObjectHashMap.this.size;
            final Object[] values = Long2ObjectHashMap.this.values;
            final int capacity = values.length;

            int i = capacity;
            if (null != values[capacity - 1])
            {
                for (i = 0; i < capacity; i++)
                {
                    if (null == values[i])
                    {
                        break;
                    }
                }
            }

            stopCounter = i;
            posCounter = i + capacity;
            isPositionValid = false;
        }
    }

    /**
     * Iterator over values.
     */
    public final class ValueIterator extends AbstractIterator<V>
    {
        /**
         * Create a new instance.
         */
        public ValueIterator()
        {
        }

        /**
         * {@inheritDoc}
         */
        public V next()
        {
            findNext();

            return unmapNullValue(values[position()]);
        }
    }

    /**
     * Iterator over keys which supports access to unboxed keys via {@link #nextLong()}.
     */
    public final class KeyIterator extends AbstractIterator<Long>
    {
        /**
         * Create a new instance.
         */
        public KeyIterator()
        {
        }

        /**
         * {@inheritDoc}
         */
        public Long next()
        {
            return nextLong();
        }

        /**
         * Return next key without boxing.
         *
         * @return next key.
         */
        public long nextLong()
        {
            findNext();

            return keys[position()];
        }
    }

    /**
     * Iterator over entries which supports access to unboxed keys via {@link #getLongKey()}.
     */
    public final class EntryIterator
        extends AbstractIterator<Entry<Long, V>>
        implements Entry<Long, V>
    {
        /**
         * Create a new instance.
         */
        public EntryIterator()
        {
        }

        /**
         * {@inheritDoc}
         */
        public Entry<Long, V> next()
        {
            findNext();
            if (shouldAvoidAllocation)
            {
                return this;
            }

            return allocateDuplicateEntry();
        }

        private Entry<Long, V> allocateDuplicateEntry()
        {
            return new MapEntry(getLongKey(), getValue());
        }

        /**
         * {@inheritDoc}
         */
        public Long getKey()
        {
            return getLongKey();
        }

        /**
         * Get key without boxing.
         *
         * @return key.
         */
        public long getLongKey()
        {
            return keys[position()];
        }

        /**
         * {@inheritDoc}
         */
        public V getValue()
        {
            return unmapNullValue(values[position()]);
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public V setValue(final V value)
        {
            final V val = (V)mapNullValue(value);
            requireNonNull(val, "value cannot be null");

            if (!this.isPositionValid)
            {
                throw new IllegalStateException();
            }

            final int pos = position();
            final Object[] values = Long2ObjectHashMap.this.values;
            final Object oldValue = values[pos];
            values[pos] = val;

            return (V)oldValue;
        }

        /**
         * An {@link java.util.Map.Entry} implementation.
         */
        public final class MapEntry implements Entry<Long, V>
        {
            private final long k;
            private final V v;

            /**
             * Create a new entry.
             *
             * @param k key.
             * @param v value.
             */
            public MapEntry(final long k, final V v)
            {
                this.k = k;
                this.v = v;
            }

            /**
             * {@inheritDoc}
             */
            public Long getKey()
            {
                return k;
            }

            /**
             * {@inheritDoc}
             */
            public V getValue()
            {
                return v;
            }

            /**
             * {@inheritDoc}
             */
            public V setValue(final V value)
            {
                return Long2ObjectHashMap.this.put(k, value);
            }

            /**
             * {@inheritDoc}
             */
            public int hashCode()
            {
                return Long.hashCode(getLongKey()) ^ (null != v ? v.hashCode() : 0);
            }

            /**
             * {@inheritDoc}
             */
            public boolean equals(final Object o)
            {
                if (!(o instanceof Map.Entry))
                {
                    return false;
                }

                final Entry<?, ?> e = (Entry<?, ?>)o;

                return (e.getKey() != null && e.getKey().equals(k)) &&
                    ((e.getValue() == null && v == null) || e.getValue().equals(v));
            }

            /**
             * {@inheritDoc}
             */
            public String toString()
            {
                return k + "=" + v;
            }
        }
    }
}
