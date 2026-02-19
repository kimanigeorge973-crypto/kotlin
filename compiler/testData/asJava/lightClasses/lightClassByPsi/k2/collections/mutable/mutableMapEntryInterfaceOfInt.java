public abstract class CMutableMapEntry /* test.CMutableMapEntry*/ implements test.IMutableMapEntry {
  @java.lang.Override()
  public static <K extends java.lang.Comparable<? super K>, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByKey();// <K extends java.lang.Comparable<? super K>, V>  comparingByKey()

  @java.lang.Override()
  public static <K, V extends java.lang.Comparable<? super V>> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByValue();// <K, V extends java.lang.Comparable<? super V>>  comparingByValue()

  @java.lang.Override()
  public static <K, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByKey(java.util.Comparator<? super K>);// <K, V>  comparingByKey(java.util.Comparator<? super K>)

  @java.lang.Override()
  public static <K, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByValue(java.util.Comparator<? super V>);// <K, V>  comparingByValue(java.util.Comparator<? super V>)

  public  CMutableMapEntry();//  .ctor()
}

public abstract class CMutableMapEntry2 /* test.CMutableMapEntry2*/ implements test.IMutableMapEntry {
  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer setValue(int);//  setValue(int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getKey();//  getKey()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getValue();//  getValue()

  @java.lang.Override()
  public static <K extends java.lang.Comparable<? super K>, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByKey();// <K extends java.lang.Comparable<? super K>, V>  comparingByKey()

  @java.lang.Override()
  public static <K, V extends java.lang.Comparable<? super V>> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByValue();// <K, V extends java.lang.Comparable<? super V>>  comparingByValue()

  @java.lang.Override()
  public static <K, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByKey(java.util.Comparator<? super K>);// <K, V>  comparingByKey(java.util.Comparator<? super K>)

  @java.lang.Override()
  public static <K, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByValue(java.util.Comparator<? super V>);// <K, V>  comparingByValue(java.util.Comparator<? super V>)

  public  CMutableMapEntry2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableMapEntry);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableMapEntry)
}

public class CMutableMapEntry3 /* test.CMutableMapEntry3*/ implements test.IMutableMapEntry {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getKey();//  getKey()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getValue();//  getValue()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer setValue(int);//  setValue(int)

  @java.lang.Override()
  public static <K extends java.lang.Comparable<? super K>, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByKey();// <K extends java.lang.Comparable<? super K>, V>  comparingByKey()

  @java.lang.Override()
  public static <K, V extends java.lang.Comparable<? super V>> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByValue();// <K, V extends java.lang.Comparable<? super V>>  comparingByValue()

  @java.lang.Override()
  public static <K, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByKey(java.util.Comparator<? super K>);// <K, V>  comparingByKey(java.util.Comparator<? super K>)

  @java.lang.Override()
  public static <K, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByValue(java.util.Comparator<? super V>);// <K, V>  comparingByValue(java.util.Comparator<? super V>)

  public  CMutableMapEntry3();//  .ctor()
}

public abstract interface IMutableMapEntry /* test.IMutableMapEntry*/ extends java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.Integer, @org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.jvm.internal.markers.KMutableMap$Entry {
}
