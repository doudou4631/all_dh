# -*- coding: utf-8 -*-
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src_path = root / "frontend/src/views/server/mark/agent/components/ProcessDetailOverview.vue"
dst_path = root / "frontend/src/views/server/mark/agent/process/detail.vue"
src = src_path.read_text(encoding="utf-8")
dst = src.replace('name="ProcessDetailOverview"', 'name="MarkAgentProcessDetail"', 1)
dst = dst.replace(
    "import AgentPastDateRangePicker from './AgentPastDateRangePicker.vue'",
    "import AgentPastDateRangePicker from '../components/AgentPastDateRangePicker.vue'",
    1,
)
dst_path.write_text(dst, encoding="utf-8")
print(f"Synced {dst_path}")
