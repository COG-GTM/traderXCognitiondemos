import type { CustomCellRendererProps } from 'ag-grid-react';

export type ButtonCellRendererProps = CustomCellRendererProps & {
    clicked: (data: unknown) => void;
};

export function ButtonCellRenderer(props: ButtonCellRendererProps) {
    return (
        <button
            className="btn btn-sm btn-info"
            onClick={() => {
                console.log(props.data);
                props.clicked(props.data);
            }}
        >
            Update
        </button>
    );
}
