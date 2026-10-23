import pytest
import yaml

from risk_lib.limits import load_limits


def test_python_object_tags_are_rejected(tmp_path):
    sentinel = tmp_path / "pwned"
    hostile = tmp_path / "limits.yaml"
    hostile.write_text(
        "limits:\n"
        f"  - counterparty: !!python/object/apply:os.system [\"touch {sentinel}\"]\n"
        "    limit: 1\n"
    )

    with pytest.raises(yaml.constructor.ConstructorError):
        load_limits(str(hostile))
    assert not sentinel.exists()


def test_plain_yaml_still_loads(tmp_path):
    good = tmp_path / "limits.yaml"
    good.write_text("limits:\n  - counterparty: TEST\n    limit: 42\n")

    assert load_limits(str(good)) == {"TEST": 42.0}
