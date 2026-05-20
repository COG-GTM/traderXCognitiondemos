package finos.traderx.accountservice.aspect;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class RepositoryTracingAspect {

    private static final Logger log = LoggerFactory.getLogger(RepositoryTracingAspect.class);
    private final Tracer tracer;

    public RepositoryTracingAspect(Tracer tracer) {
        this.tracer = tracer;
    }

    @Around("execution(* finos.traderx.accountservice.repository.*.*(..))")
    public Object traceRepositoryCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String spanName = "repository." + className + "." + methodName;

        Span span = tracer.spanBuilder(spanName).startSpan();
        try {
            span.setAttribute("db.operation", methodName);
            span.setAttribute("db.repository", className);
            if (joinPoint.getArgs().length > 0) {
                span.setAttribute("db.parameters", Arrays.toString(joinPoint.getArgs()));
            }
            log.debug("Repository call: {}({})", methodName, Arrays.toString(joinPoint.getArgs()));

            Object result = joinPoint.proceed();

            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
