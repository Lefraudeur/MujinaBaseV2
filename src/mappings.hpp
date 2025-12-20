#pragma once

#include "meta_jni.hpp"
#include<string>

namespace maps
{
	KLASS_DECLARATION(Class, "java/lang/Class");
	KLASS_DECLARATION(String, "java/lang/String");

	BEGIN_KLASS_DEF(Object, "java/lang/Object")
	END_KLASS_DEF()

	BEGIN_KLASS_DEF(Type, "java/lang/reflect/Type")
		jni::method<String, "getTypeName"> getTypeName{ *this };
	END_KLASS_DEF()

	BEGIN_KLASS_DEF(Field, "java/lang/reflect/Field")
		jni::method<Class, "getType"> getType{*this};
		jni::method<Type, "getGenericType"> getGenericType{ *this };
		jni::method<String, "toGenericString"> toGenericString{ *this };
	END_KLASS_DEF()

	BEGIN_KLASS_MEMBERS(Class)
		operator jclass() const
		{
			return (jclass)object_instance;
		}

		jni::method<jni::array<Field>, "getDeclaredFields"> getDeclaredFields{ *this };
		jni::method<String, "getName"> getName{ *this };
		jni::method<String, "getTypeName"> getTypeName{ *this };
	END_KLASS_MEMBERS()

	BEGIN_KLASS_MEMBERS(String)
		static String create(const char* str)
		{
			return String(jni::get_env()->NewStringUTF(str));
		}


		std::string to_string()
		{
			if (!object_instance) return std::string();
			jstring str_obj = (jstring)object_instance;
			jsize utf8_size = jni::get_env()->GetStringUTFLength(str_obj);
			jsize size = jni::get_env()->GetStringLength(str_obj);

			std::string str(utf8_size, '\0');
			jni::get_env()->GetStringUTFRegion(str_obj, 0, size, str.data());
			return str;
		}
	END_KLASS_MEMBERS()

	BEGIN_KLASS_DEF(Collection, "java/util/Collection")
		jni::method<jni::array<Object>, "toArray"> toArray{ *this };
	END_KLASS_DEF()
	BEGIN_KLASS_DEF_EX(List, "java/util/List", Collection)
	END_KLASS_DEF()

	BEGIN_KLASS_DEF(URL, "java/net/URL")
		jni::constructor<String> constructor{ *this };

		jni::method<String, "toString"> toString{ *this };
	END_KLASS_DEF()

	BEGIN_KLASS_DEF(ClassLoader, "java/lang/ClassLoader")
		jni::field<ClassLoader, "parent"> parent{ *this };
		jni::method<Class, "loadClass", jni::NOT_STATIC, String> loadClass{ *this };
	END_KLASS_DEF()

	BEGIN_KLASS_DEF_EX(URLClassLoader, "java/net/URLClassLoader", ClassLoader)
		jni::constructor<jni::array<URL>> constructor{ *this };
		jni::constructor<jni::array<URL>, ClassLoader> constructor2{ *this };

		jni::method<void, "addURL", jni::NOT_STATIC, URL> addURL{ *this };
		jni::method<Class, "findClass", jni::NOT_STATIC, String> findClass{ *this };
		jni::method<jni::array<URL>, "getURLs", jni::NOT_STATIC> getURLs{ *this };
	END_KLASS_DEF()

	BEGIN_KLASS_DEF_EX(SecureClassLoader, "java/security/SecureClassLoader", ClassLoader)
		jni::constructor<> constructor{*this};
		jni::constructor<ClassLoader> constructor2{ *this };
	END_KLASS_DEF()

	BEGIN_KLASS_DEF_EX(MemoryJarClassLoader, "io/github/lefraudeur/internal/MemoryJarClassLoader", ClassLoader)
		jni::constructor<jni::array<jbyte>, ClassLoader> constructor{ *this };
	END_KLASS_DEF()

	BEGIN_KLASS_DEF(Main, "io/github/lefraudeur/Main")
		jni::method<void, "onLoad", jni::STATIC> onLoad{ *this };
		jni::method<void, "onUnload", jni::STATIC> onUnload{ *this };
		//jni::method<String, "getWindowTitle", jni::STATIC> getWindowTitle{ *this };
	END_KLASS_DEF()

	BEGIN_KLASS_DEF_EX(EventClassLoader, "io/github/lefraudeur/internal/EventClassLoader", ClassLoader)
		jni::constructor<ClassLoader, MemoryJarClassLoader> constructor{ *this };
	END_KLASS_DEF()

	BEGIN_KLASS_DEF(ClassModifier, "io/github/lefraudeur/internal/patcher/ClassModifier")
		jni::method<jni::array<jbyte>, "patch", jni::NOT_STATIC, jni::array<jbyte>> patch{ *this };
	END_KLASS_DEF()

	BEGIN_KLASS_DEF(PatcherHelper, "io/github/lefraudeur/internal/patcher/PatcherHelper")
		jni::method<jboolean, "init", jni::STATIC> init{ *this };
		jni::method<jni::array<Class>, "getClassesToTransform", jni::STATIC> getClassesToTransform{ *this };
		jni::method<ClassModifier, "getClassModifier", jni::STATIC, Class> getClassModifier{ *this };
	END_KLASS_DEF()
}