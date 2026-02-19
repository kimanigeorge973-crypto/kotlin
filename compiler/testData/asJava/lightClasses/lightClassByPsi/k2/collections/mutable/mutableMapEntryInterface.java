public abstract class CMutableMapEntry /* test.CMutableMapEntry*/<KElem, VElem>  implements test.IMutableMapEntry<KElem, VElem> {
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

public abstract class CMutableMapEntry2 /* test.CMutableMapEntry2*/<KElem, VElem>  implements test.IMutableMapEntry<KElem, VElem> {
  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public VElem setValue(VElem);//  setValue(VElem)

  @java.lang.Override()
  public KElem getKey();//  getKey()

  @java.lang.Override()
  public VElem getValue();//  getValue()

  @java.lang.Override()
  public static <K extends java.lang.Comparable<? super K>, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByKey();// <K extends java.lang.Comparable<? super K>, V>  comparingByKey()

  @java.lang.Override()
  public static <K, V extends java.lang.Comparable<? super V>> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByValue();// <K, V extends java.lang.Comparable<? super V>>  comparingByValue()

  @java.lang.Override()
  public static <K, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByKey(java.util.Comparator<? super K>);// <K, V>  comparingByKey(java.util.Comparator<? super K>)

  @java.lang.Override()
  public static <K, V> java.util.Comparator<java.util.Map.Entry<K, V>> comparingByValue(java.util.Comparator<? super V>);// <K, V>  comparingByValue(java.util.Comparator<? super V>)

  public  CMutableMapEntry2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableMapEntry<KElem, VElem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableMapEntry<KElem, VElem>)
}

public class CMutableMapEntry3 /* test.CMutableMapEntry3*/<KElem, VElem>  implements test.IMutableMapEntry<KElem, VElem> {
  @java.lang.Override()
  public KElem getKey();//  getKey()

  @java.lang.Override()
  public VElem getValue();//  getValue()

  @java.lang.Override()
  public VElem setValue(VElem);//  setValue(VElem)

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

public abstract interface IMutableMapEntry /* test.IMutableMapEntry*/<KElem, VElem>  extends java.util.Map.Entry<KElem, VElem>, kotlin.jvm.internal.markers.KMutableMap$Entry {
}
