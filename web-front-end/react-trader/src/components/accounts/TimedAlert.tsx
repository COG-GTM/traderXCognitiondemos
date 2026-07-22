import { useEffect } from 'react';
import type { ReactNode } from 'react';

interface TimedAlertProps {
    type: 'success' | 'danger';
    onClosed: () => void;
    dismissTimeout?: number;
    children: ReactNode;
}

export function TimedAlert({ type, onClosed, dismissTimeout = 2000, children }: TimedAlertProps) {
    useEffect(() => {
        const timer = setTimeout(onClosed, dismissTimeout);
        return () => clearTimeout(timer);
    }, [onClosed, dismissTimeout]);

    return (
        <div className={`alert alert-${type} alert-dismissible`} role="alert">
            {children}
            <button type="button" className="btn-close" aria-label="Close" onClick={onClosed}></button>
        </div>
    );
}
