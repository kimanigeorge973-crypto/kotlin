public abstract class CMapEntry /* test.CMapEntry*/ implements test.IMapEntry {
  @java.lang.Override()
  public int setValue(int);//  setValue(int)

  @java.lang.Override()
  public static <K extends java.lang.Comparable<? super K>, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByKey();// <K extends java.lang.Comparable<? super K>, V>  comparingByKey()

  @java.lang.Override()
  public static <K, V extends java.lang.Comparable<? super V>> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByValue();// <K, V extends java.lang.Comparable<? super V>>  comparingByValue()

  @java.lang.Override()
  public static <K, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByKey(java.util.Comparator<? super K>);// <K, V>  comparingByKey(java.util.Comparator<? super K>)

  @java.lang.Override()
  public static <K, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByValue(java.util.Comparator<? super V>);// <K, V>  comparingByValue(java.util.Comparator<? super V>)

  public  CMapEntry();//  .ctor()
}

public abstract class CMapEntry2 /* test.CMapEntry2*/ implements test.IMapEntry {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getKey();//  getKey()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getValue();//  getValue()

  @java.lang.Override()
  public int setValue(int);//  setValue(int)

  @java.lang.Override()
  public static <K extends java.lang.Comparable<? super K>, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByKey();// <K extends java.lang.Comparable<? super K>, V>  comparingByKey()

  @java.lang.Override()
  public static <K, V extends java.lang.Comparable<? super V>> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByValue();// <K, V extends java.lang.Comparable<? super V>>  comparingByValue()

  @java.lang.Override()
  public static <K, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByKey(java.util.Comparator<? super K>);// <K, V>  comparingByKey(java.util.Comparator<? super K>)

  @java.lang.Override()
  public static <K, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByValue(java.util.Comparator<? super V>);// <K, V>  comparingByValue(java.util.Comparator<? super V>)

  public  CMapEntry2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMapEntry);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMapEntry)
}

public class CMapEntry3 /* test.CMapEntry3*/ implements test.IMapEntry {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getKey();//  getKey()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getValue();//  getValue()

  @java.lang.Override()
  public int setValue(int);//  setValue(int)

  @java.lang.Override()
  public static <K extends java.lang.Comparable<? super K>, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByKey();// <K extends java.lang.Comparable<? super K>, V>  comparingByKey()

  @java.lang.Override()
  public static <K, V extends java.lang.Comparable<? super V>> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByValue();// <K, V extends java.lang.Comparable<? super V>>  comparingByValue()

  @java.lang.Override()
  public static <K, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByKey(java.util.Comparator<? super K>);// <K, V>  comparingByKey(java.util.Comparator<? super K>)

  @java.lang.Override()
  public static <K, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByValue(java.util.Comparator<? super V>);// <K, V>  comparingByValue(java.util.Comparator<? super V>)

  public  CMapEntry3();//  .ctor()
}

public abstract interface IMapEntry /* test.IMapEntry*/ extends java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.Integer, @org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.jvm.internal.markers.KMappedMarker {
}
