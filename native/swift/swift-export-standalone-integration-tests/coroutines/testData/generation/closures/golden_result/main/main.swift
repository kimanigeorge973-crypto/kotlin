@_implementationOnly import KotlinBridges_main
import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport

public func accept_suspend_function_type(
    block: @escaping () async throws -> Swift.Int32
) -> Swift.Int32 {
    return __root___accept_suspend_function_type__TypesOfArguments__U282920asyncU20throwsU202D_U20Swift_Int32__({
        let originalBlock = block
        return { __continuationPtr, __exceptionPtr, __cancellationPtr in
            let __continuation: (Swift.Int32) -> Swift.Void = {
        let pointerToBlock = __continuationPtr
        return { _1 in return main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(pointerToBlock, _1) }
    }()
            let __exception: (Swift.Error) -> Swift.Void = {
        let pointerToBlock = __exceptionPtr
        return { _1 in return main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock, _1) }
    }()
            Task {
                do {
                    let result = try await originalBlock()
                    __continuation(result)
                } catch {
                    __exception(error)
                }
            }
        }
    }())
}
