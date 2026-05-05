import os
from pathlib import Path

from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_ui_route_serves_index_from_any_working_directory(tmp_path: Path) -> None:
    previous = Path.cwd()
    try:
        os.chdir(tmp_path)
        response = client.get("/")
    finally:
        os.chdir(previous)

    assert response.status_code == 200
    assert "function renderMissionLaunch()" in response.text
    assert "Mission-led" in response.text or "Coach Studio" in response.text
