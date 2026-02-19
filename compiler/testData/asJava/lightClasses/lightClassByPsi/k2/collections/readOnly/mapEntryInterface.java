public abstract class CMapEntry /* test.CMapEntry*/<KElem, VElem>  implements test.IMapEntry<KElem, VElem> {
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

  public  CMapEntry();//  .ctor()
}

public abstract class CMapEntry2 /* test.CMapEntry2*/<KElem, VElem>  implements test.IMapEntry<KElem, VElem> {
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

  public  CMapEntry2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMapEntry<KElem, VElem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMapEntry<KElem, VElem>)
}

public class CMapEntry3 /* test.CMapEntry3*/<KElem, VElem>  implements test.IMapEntry<KElem, VElem> {
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

  public  CMapEntry3();//  .ctor()
}

public abstract interface IMapEntry /* test.IMapEntry*/<KElem, VElem>  extends java.util.Map.Entry<KElem, VElem>, kotlin.jvm.internal.markers.KMappedMarker {
}
