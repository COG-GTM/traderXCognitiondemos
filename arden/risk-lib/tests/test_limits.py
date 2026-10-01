from risk_lib.limits import load_limits


def test_loads_limits():
    limits = load_limits("config/limits.yaml")
    assert limits["BARC"] == 250_000_000.0


def test_limits_loader_rejects_python_tags(tmp_path):
    import pytest
    import yaml

    hostile = tmp_path / "limits.yaml"
    hostile.write_text("limits: !!python/object/apply:os.system ['echo pwned']\n")
    with pytest.raises(yaml.constructor.ConstructorError):
        load_limits(str(hostile))
