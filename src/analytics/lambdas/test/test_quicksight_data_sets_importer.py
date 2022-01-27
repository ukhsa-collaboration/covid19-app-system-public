from . import check_code


def test_code_is_valid():
    assert check_code('importers/quicksight_data_sets_importer.py')
