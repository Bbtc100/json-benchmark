import pandas as pd
import plotly.graph_objects as go
from pathlib import Path

CSV_FILE = "benchmark_summary.csv"
OUTPUT_DIR = Path("plots")

OUTPUT_DIR.mkdir(exist_ok=True)

COLORS = {
    "java_single": "#1f77b4",
    "java_multi": "#ff7f0e",
    "python": "#2ca02c",
    "jq": "#d62728",
}


def normalize_dataset(name: str):
    parts = name.split("_")

    if len(parts) == 1:
        return name

    if parts[-1].isdigit():
        return "_".join(parts[:-1])

    return name


def create_chart(df, operation, variant):

    chart_df = df[
        (df["operation"] == operation)
        & (df["variant"] == variant)
    ].copy()

    if chart_df.empty:
        return

    base_order = ["data_10k", "data_100k", "data_1M", "data_10M"]

    chart_df["dataset_base"] = chart_df["dataset"].apply(normalize_dataset)

    chart_df = chart_df[
        chart_df["dataset_base"].isin(base_order)
    ].copy()

    chart_df["dataset_base"] = pd.Categorical(
        chart_df["dataset_base"],
        categories=base_order,
        ordered=True
    )

    chart_df = chart_df.sort_values("dataset_base")

    fig = go.Figure()

    for tool in sorted(chart_df["tool"].unique()):

        tool_df = chart_df[chart_df["tool"] == tool].copy()

        tool_df["dataset_base"] = tool_df["dataset"].apply(normalize_dataset)

        tool_df["sort_key"] = tool_df["dataset_base"].apply(
            lambda x: base_order.index(x) if x in base_order else 999
        )

        tool_df = tool_df.sort_values("sort_key").drop(columns=["sort_key"])

        if tool_df.empty:
            continue

        is_10m = tool_df["dataset"].astype(str).str.contains("10M")

        normal_df = tool_df[~is_10m]

        if not normal_df.empty:
            fig.add_trace(
                go.Bar(
                    name=f"{tool}",
                    hovertemplate="Dataset: %{x}<br>Runtime: %{y:.2f} ms<extra></extra>",
                    x=normal_df["dataset_base"],
                    y=normal_df["mean_ms"],
                    error_y=dict(
                        type="data",
                        array=normal_df["stddev_ms"],
                        visible=True
                    ),
                    marker_color=COLORS.get(tool),
                    visible=True
                )
            )

        large_df = tool_df[is_10m]

        if not large_df.empty:
            fig.add_trace(
                go.Bar(
                    name=f"{tool} (10M)",
                    hovertemplate="Dataset: %{x}<br>Runtime: %{y:.2f} ms<extra></extra>",
                    x=large_df["dataset_base"],
                    y=large_df["mean_ms"],
                    error_y=dict(
                        type="data",
                        array=large_df["stddev_ms"],
                        visible=True
                    ),
                    marker_color=COLORS.get(tool),
                    visible="legendonly"
                )
            )

    fig.update_layout(
        title=f"{operation} ({variant})",
        xaxis_title="Dataset",
        yaxis_title="Runtime (ms)",
        barmode="group",
        template="plotly_white",
        legend_title="Implementation",
        hovermode="x unified"
    )

    filename = OUTPUT_DIR / f"{operation}_{variant}.html"

    fig.write_html(filename, include_plotlyjs="cdn")

    print("Generated:", filename)

def main():

    df = pd.read_csv(CSV_FILE)

    operations = sorted(df["operation"].unique())
    variants = sorted(df["variant"].unique())

    for operation in operations:
        for variant in variants:
            create_chart(
                df,
                operation,
                variant
            )


if __name__ == "__main__":
    main()