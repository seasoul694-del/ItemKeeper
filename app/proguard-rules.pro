# 与官方 Catalog 相同：使用默认模板，规则由各库（backdrop/compose）自带 consumer rules 提供。
# 补充：ThemeMode 等枚举经 valueOf 反射读取，R8 混淆枚举成员名会导致设置页崩溃。
-keepclassmembers enum com.tang.itemkeeper.data.ThemeMode {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
