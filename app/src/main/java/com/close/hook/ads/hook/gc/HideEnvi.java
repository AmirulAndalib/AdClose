package com.close.hook.ads.hook.gc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import android.system.Os;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/*
 * 2023.12.8-10:14
 * 参考 https://d0nuts33.github.io/2023/04/29/加固防护总结
 */

public class HideEnvi {

	public static void handle() {
		try {
			XposedBridge.hookMethod(File.class.getDeclaredMethod("exists"), new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					String path = ((File) param.thisObject).getAbsolutePath();
					if (isXposedOrMagiskPath(path) || isLibRiruloaderSo(path)) {
						param.setResult(false);
					}
				}

				private boolean isXposedOrMagiskPath(String path) {
					String[] paths = { "/sbin/.magisk", "/system/bin/magisk", "/data/data/com.topjohnwu.magisk",
							"/system/lib/libriruloader.so", "/system/bin/su", "/system/xbin/su", "/system/sbin/su",
							"/sbin/su", "/vendor/bin/su", "xposed.installer", "app_process_xposed", "libriru_",
							"/data/misc/edxp_", "libxposed_art.so", "libriruloader.so", "app_process_zposed" };
					return Arrays.asList(paths).contains(path);
				}

				private boolean isLibRiruloaderSo(String path) {
					return "/system/lib/libriruloader.so".equals(path);
				}
			});

	    /*
			XposedBridge.hookAllMethods(Runtime.class, "exec", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (detectXposedOrMagiskInMemory() || isRiruActive()) {
						param.setResult(null);
					}
				}

				private boolean detectXposedOrMagiskInMemory() {
					try {
						String[] magiskFeatures64 = { "MAGISK_INJ_" };
						String[] xposedFeatures = { "libriru_", "/data/misc/edxp_", "libxposed_art.so",
								"libriruloader.so", "app_process_zposed" };

						BufferedReader reader = new BufferedReader(new FileReader("/proc/self/maps"));
						String line;
						while ((line = reader.readLine()) != null) {
							for (String feature : magiskFeatures64) {
									if (line.contains(feature)) {
										reader.close();
										return true;
									}
								}
							for (String feature : xposedFeatures) {
								if (line.contains(feature)) {
									reader.close();
									return true;
								}
							}
						}
						reader.close();
					} catch (Exception e) {
						XposedBridge.log("HideEnvi Error: " + e.getMessage());
					}
					return false;
				}

				private String getSystemProperty(String key, String defaultValue) {
					try {
						Class<?> systemProperties = Class.forName("android.os.SystemProperties");
						return (String) systemProperties.getMethod("get", String.class, String.class)
								.invoke(systemProperties, key, defaultValue);
					} catch (Exception e) {
						return defaultValue;
					}
				}

				private boolean isRiruActive() {
					String riruBridge = getSystemProperty("ro.dalvik.vm.native.bridge", "");
					return !riruBridge.isEmpty() && new File(riruBridge).exists();
				}
			});

        */

		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}
}
