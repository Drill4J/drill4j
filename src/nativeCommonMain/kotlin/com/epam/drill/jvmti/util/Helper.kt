package com.epam.drill.jvmti.util

import com.epam.drill.jvmti.logger.DLogger
import jvmapi.*
import kotlinx.cinterop.useContents

val logger
    get() = DLogger("capabilities")

fun printAllowedCapabilities() {
    val potentialCapabilities = GetPotentialCapabilities()
    potentialCapabilities.useContents {
        logger.info { "--------------------------Allowed Capabilities--------------------------" }
        logger.info { this::can_access_local_variables.name + " = " + can_access_local_variables }
        logger.info { this::can_force_early_return.name + " = " + can_force_early_return }
        logger.info { this::can_generate_all_class_hook_events.name + " = " + can_generate_all_class_hook_events }
        logger.info { this::can_generate_breakpoint_events.name + " = " + can_generate_breakpoint_events }
        logger.info { this::can_generate_compiled_method_load_events.name + " = " + can_generate_compiled_method_load_events }
        logger.info { this::can_generate_exception_events.name + " = " + can_generate_exception_events }
        logger.info { this::can_generate_field_access_events.name + " = " + can_generate_field_access_events }
        logger.info { this::can_generate_field_modification_events.name + " = " + can_generate_field_modification_events }
        logger.info { this::can_generate_frame_pop_events.name + " = " + can_generate_frame_pop_events }
        logger.info { this::can_generate_garbage_collection_events.name + " = " + can_generate_garbage_collection_events }
        logger.info { this::can_generate_method_entry_events.name + " = " + can_generate_method_entry_events }
        logger.info { this::can_generate_method_exit_events.name + " = " + can_generate_method_exit_events }
        logger.info { this::can_generate_monitor_events.name + " = " + can_generate_monitor_events }
        logger.info { this::can_generate_native_method_bind_events.name + " = " + can_generate_native_method_bind_events }
        logger.info { this::can_generate_object_free_events.name + " = " + can_generate_object_free_events }
        logger.info { this::can_generate_resource_exhaustion_heap_events.name + " = " + can_generate_resource_exhaustion_heap_events }
        logger.info { this::can_generate_resource_exhaustion_threads_events.name + " = " + can_generate_resource_exhaustion_threads_events }
        logger.info { this::can_generate_single_step_events.name + " = " + can_generate_single_step_events }
        logger.info { this::can_generate_vm_object_alloc_events.name + " = " + can_generate_vm_object_alloc_events }
        logger.info { this::can_get_bytecodes.name + " = " + can_get_bytecodes }
        logger.info { this::can_get_constant_pool.name + " = " + can_get_constant_pool }
        logger.info { this::can_get_current_contended_monitor.name + " = " + can_get_current_contended_monitor }
        logger.info { this::can_get_current_thread_cpu_time.name + " = " + can_get_current_thread_cpu_time }
        logger.info { this::can_get_line_numbers.name + " = " + can_get_line_numbers }
        logger.info { this::can_get_monitor_info.name + " = " + can_get_monitor_info }
        logger.info { this::can_get_owned_monitor_info.name + " = " + can_get_owned_monitor_info }
        logger.info { this::can_get_owned_monitor_stack_depth_info.name + " = " + can_get_owned_monitor_stack_depth_info }
        logger.info { this::can_get_source_debug_extension.name + " = " + can_get_source_debug_extension }
        logger.info { this::can_get_source_file_name.name + " = " + can_get_source_file_name }
        logger.info { this::can_get_synthetic_attribute.name + " = " + can_get_synthetic_attribute }
        logger.info { this::can_get_thread_cpu_time.name + " = " + can_get_thread_cpu_time }
        logger.info { this::can_maintain_original_method_order.name + " = " + can_maintain_original_method_order }
        logger.info { this::can_pop_frame.name + " = " + can_pop_frame }
        logger.info { this::can_redefine_any_class.name + " = " + can_redefine_any_class }
        logger.info { this::can_redefine_classes.name + " = " + can_redefine_classes }
        logger.info { this::can_retransform_any_class.name + " = " + can_retransform_any_class }
        logger.info { this::can_retransform_classes.name + " = " + can_retransform_classes }
        logger.info { this::can_set_native_method_prefix.name + " = " + can_set_native_method_prefix }
        logger.info { this::can_signal_thread.name + " = " + can_signal_thread }
        logger.info { this::can_suspend.name + " = " + can_suspend }
        logger.info { this::can_tag_objects.name + " = " + can_tag_objects }

    }
}


