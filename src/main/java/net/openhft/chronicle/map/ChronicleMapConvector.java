package net.openhft.chronicle.map;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import net.openhft.lang.model.DataValueClasses;
import net.openhft.lang.model.constraints.NotNull;
import net.openhft.lang.values.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;

import static java.beans.Introspector.getBeanInfo;

/**
 * @author Rob Austin.
 */
public class ChronicleMapConvector<K, V> implements Converter {

    private static final Logger LOG = LoggerFactory.getLogger(VanillaChronicleMap.class);

    private final Class entrySetClass;
    private final Class writeThroughEntryClass;
    private final Map<K, V> map;

    public ChronicleMapConvector(@NotNull Map map) {

        final String vanillaChronicleMap = VanillaChronicleMap.class.getCanonicalName();

        try {
            this.entrySetClass = Class.forName(vanillaChronicleMap + "$EntrySet");
            this.writeThroughEntryClass = Class.forName(vanillaChronicleMap + "$WriteThroughEntry");
        } catch (ClassNotFoundException e) {
            throw new ConversionException("", e);
        }

        this.map = map;
    }


    @Override
    public boolean canConvert(Class aClass) {

        if (entrySetClass.isAssignableFrom(aClass) ||
                writeThroughEntryClass.isAssignableFrom(aClass))
            return true;

        String canonicalName = aClass.getCanonicalName();

        return canonicalName.startsWith("net.openhft.lang.values") ||
                canonicalName.endsWith("$$Native") ||
                canonicalName.endsWith("$$Heap");
    }

    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext
            marshallingContext) {

        if (writeThroughEntryClass.isAssignableFrom(o.getClass())) {

            final AbstractMap.SimpleEntry e = (AbstractMap.SimpleEntry) o;
            Object key = e.getKey();
            writer.startNode(key.getClass().getCanonicalName());


            marshallingContext.convertAnother(key);
            writer.endNode();

            Object value = e.getValue();
            writer.startNode(value.getClass().getCanonicalName());


            marshallingContext.convertAnother(value);
            writer.endNode();

        } else if (entrySetClass.isAssignableFrom(o.getClass())) {

            for (Map.Entry e : (Iterable<Map.Entry>) o) {
                writer.startNode("entry");
                marshallingContext.convertAnother(e);
                writer.endNode();
            }

        }

        final String canonicalName = o.getClass().getCanonicalName();

        boolean isNative = canonicalName.endsWith("$$Native");
        boolean isHeap = canonicalName.endsWith("$$Heap");

        if (isNative || isHeap) {
            if (canonicalName.startsWith("net.openhft.lang.values")) {

                Method[] methods = o.getClass().getMethods();

                for (Method method : methods) {

                    if (!method.getName().equals("getValue") ||
                            method.getParameterTypes().length != 0) {
                        continue;
                    }

                    try {
                        marshallingContext.convertAnother(method.invoke(o));
                        return;
                    } catch (Exception e) {
                        throw new ConversionException("class=" + canonicalName, e);
                    }
                }

                throw new ConversionException("class=" + canonicalName);

            }


            try {

                final BeanInfo info = getBeanInfo(o.getClass());  //  new BeanGenerator

                for (PropertyDescriptor p : info.getPropertyDescriptors()) {

                    if (p.getName().equals("Class") ||
                            p.getReadMethod() == null ||
                            p.getWriteMethod() == null)
                        continue;

                    try {

                        final Method readMethod = p.getReadMethod();
                        final Object value = readMethod.invoke(o);

                        if (value == null)
                            return;

                        writer.startNode(p.getDisplayName());
                        marshallingContext.convertAnother(value);
                        writer.endNode();

                    } catch (Exception e) {
                        LOG.error("class=" + p.getName(), e);
                    }


                }
            } catch (IntrospectionException e) {
                throw new ConversionException("class=" + canonicalName, e);
            }

        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {


        if ("cmap".equals(reader.getNodeName())) {

            while (reader.hasMoreChildren()) {
                reader.moveDown();
                final String nodeName0 = reader.getNodeName();

                if (!nodeName0.equals("entry"))
                    throw new ConversionException("unable to convert node " +
                            "named=" + nodeName0);
                final K k;
                final V v;

                reader.moveDown();
                k = (K) deserialize(context, reader);
                reader.moveUp();

                reader.moveDown();
                v = (V) deserialize(context, reader);
                reader.moveUp();


                if (k != null)
                    map.put(k, v);

                reader.moveUp();
            }

            return null;

        }

        final String canonicalName = context.getRequiredType().getCanonicalName();

        boolean isNative = canonicalName.endsWith("$$Native");
        boolean isHeap = canonicalName.endsWith("$$Heap");

        if (!isNative && !isHeap)
            return null;

        if (context.getRequiredType().getCanonicalName().startsWith
                ("net.openhft.lang.values"))
            return toNativeValueObjects(reader, context.getRequiredType());

        final String nodeName = isNative ?

                canonicalName.substring(0, canonicalName.length() -
                        "$$Native".length()) :

                canonicalName.substring(0, canonicalName.length() -
                        "$$Heap".length());

        try {

            final Class<?> interfaceClass = Class.forName(nodeName);

            final Object result = (isNative) ?
                    DataValueClasses.newDirectInstance(interfaceClass) :
                    DataValueClasses.newInstance(interfaceClass);

            final BeanInfo beanInfo = getBeanInfo(result.getClass());

            while (reader.hasMoreChildren()) {
                reader.moveDown();

                final String name = reader.getNodeName();

                for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {

                    if (!descriptor.getName().equals(name))
                        continue;

                    final Class<?>[] parameterTypes = descriptor.getWriteMethod()
                            .getParameterTypes();

                    if (parameterTypes.length != 1)
                        continue;

                    final Object object = context.convertAnother(null, parameterTypes[0]);

                    try {
                        descriptor.getWriteMethod().invoke(result, object);
                    } catch (Exception e) {
                        LOG.error("", e);
                    }

                    break;
                }

                reader.moveUp();
            }

            return result;

        } catch (Exception e) {
            throw new ConversionException("class=" + canonicalName, e);
        }


    }


    static Object deserialize(@NotNull UnmarshallingContext unmarshallingContext,
                              @NotNull HierarchicalStreamReader reader) {

        final String clazz = reader.getNodeName();

        switch (clazz) {

            case "java.util.Collections$EmptySet":
                return Collections.EMPTY_SET;

            case "java.util.Collections$EmptyList":
                return Collections.EMPTY_LIST;

            case "java.util.Collections$EmptyMap":
            case "java.util.Collections.EmptyMap":
                return Collections.EMPTY_MAP;

        }

        return unmarshallingContext.convertAnother(null, forName(clazz));
    }

    static Class forName(String clazz) {

        try {

            return Class.forName(clazz);

        } catch (ClassNotFoundException e) {

            boolean isNative = clazz.endsWith("$$Native");
            boolean isHeap = clazz.endsWith("$$Heap");

            if (!isNative && !isHeap)
                throw new ConversionException("class=" + clazz, e);

            final String nativeInterface = isNative ? clazz.substring(0, clazz.length() -
                    "$$Native".length()) : clazz.substring(0, clazz.length() -
                    "$$Heap".length());
            try {
                DataValueClasses.newDirectInstance(Class.forName(clazz));
                return Class.forName(nativeInterface);
            } catch (Exception e1) {
                throw new ConversionException("class=" + clazz, e1);
            }

        }
    }


    static Object toNativeValueObjects(HierarchicalStreamReader reader, final Class aClass) {

        final Object o = DataValueClasses.newDirectInstance(aClass);

        try {

            final BeanInfo info = Introspector.getBeanInfo(o.getClass());  //  new BeanGenerator

            for (PropertyDescriptor p : info.getPropertyDescriptors()) {

                if (!p.getName().equals("value")) {
                    continue;
                }

                final String value = reader.getValue();
                Class<?> parameterType = p.getPropertyType();

                if (StringValue.class.isAssignableFrom(o.getClass())) {
                    ((StringValue) o).setValue(value);
                    return o;
                }

                if (parameterType.isPrimitive()) {

                    // convert the primitive to their boxed type

                    if (parameterType == int.class)
                        parameterType = Integer.class;
                    else if (parameterType == char.class)
                        parameterType = Character.class;
                    else {
                        final String name = parameterType.getSimpleName();

                        final String properName = "java.lang." + Character.toString
                                (name.charAt(0)).toUpperCase()
                                + name.substring(1);
                        parameterType = Class.forName(properName);
                    }
                }

                final Method valueOf = parameterType.getMethod("valueOf", String.class);
                final Object boxedValue = valueOf.invoke(null, value);

                p.getWriteMethod().invoke(o, boxedValue);
                return o;

            }

        } catch (Exception e) {
            //
        }

        throw new ConversionException("setValue(..) method not found in class=" +
                aClass.getCanonicalName());
    }

}
