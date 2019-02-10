//package com.epam.drill.plugin.api.processing
//
//import com.epam.drill.storage.PluginsStorage
//import net.bytebuddy.ByteBuddy
//import net.bytebuddy.agent.ByteBuddyAgent
//import net.bytebuddy.asm.Advice
//import net.bytebuddy.asm.AsmVisitorWrapper
//import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
//import net.bytebuddy.jar.asm.ClassWriter
//import net.bytebuddy.matcher.ElementMatchers
//import java.lang.reflect.Method
//
//abstract class ByteBuddyPlugin : AgentPluginPart() {
//    companion object {
//        var process: ByteBuddyPlugin? = null
//    }
//
//    init {
//        process = this
//        applyInstrumentRules()
//    }
//
//
//    fun applyInstrumentRules() {
//        ByteBuddyAgent.install()
//        ByteBuddy()
//                .redefine(monitoredClass)
//                .visit(AsmVisitorWrapper.ForDeclaredMethods()
//                        .writerFlags(ClassWriter.COMPUTE_FRAMES)
//                        .method(ElementMatchers.named(monitoredMethod)!!, Advice.to(EnterAdvice.javaClass)))
//
//                .make()
//                .load(monitoredClass.classLoader, ClassReloadingStrategy.fromInstalledAgent())
//    }
//
//    override fun unload() {
//        ByteBuddy()
//                .redefine(monitoredClass)
//                .make()
//                .load(monitoredClass.classLoader, ClassReloadingStrategy.fromInstalledAgent())
//        pluginInfo().enabled = false
//        PluginsStorage.pluginsMapping.remove(pluginInfo().id)
//    }
//
//    abstract fun processDataBeforeSending(thiss: Any, method: Method, parameters: Array<Any>): String?
//
//    abstract val monitoredClass: Class<*>
//    abstract val monitoredMethod: String
//
//    private object EnterAdvice {
//
//        @Advice.OnMethodEnter
//        @JvmStatic
//        @Throws(Exception::class)
//        fun enter(@Advice.This thsss: Any, @Advice.Origin method: Method, @Advice.AllArguments parameters: Array<Any>): Long {
//            process?.sendData(process?.processDataBeforeSending(thsss, method, parameters))
//            return System.currentTimeMillis()
//        }
//    }
//
//}
