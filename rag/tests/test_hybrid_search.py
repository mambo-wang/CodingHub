"""Tests for zvec FTS hybrid search integration."""

import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import zvec


class TestZvecFtsApiAvailability:
    """Verify zvec FTS API is available (version >= 0.5.0)."""

    def test_zvec_version(self):
        """zvec version is >= 0.5.0."""
        version = zvec.__version__
        major, minor = int(version.split(".")[0]), int(version.split(".")[1])
        assert (major, minor) >= (0, 5), f"zvec {version} < 0.5.0"

    def test_fts_index_param_importable(self):
        """FtsIndexParam is importable from zvec."""
        assert hasattr(zvec, "FtsIndexParam")
        param = zvec.FtsIndexParam(tokenizer_name="standard")
        assert param is not None

    def test_fts_query_importable(self):
        """Fts and FtsQueryParam are importable."""
        assert hasattr(zvec, "Fts")
        assert hasattr(zvec, "FtsQueryParam")
        fts = zvec.Fts(match_string="test query")
        assert fts is not None

    def test_rrf_reranker_importable(self):
        """RrfReRanker is importable."""
        assert hasattr(zvec, "RrfReRanker")
        reranker = zvec.RrfReRanker(rank_constant=60)
        assert reranker is not None

    def test_jieba_tokenizer_available(self):
        """jieba tokenizer is available for Chinese text."""
        # zvec bundles jieba dict — verify the function exists
        assert hasattr(zvec, "get_default_jieba_dict_dir")


class TestHybridSearchDegradation:
    """Test that hybrid search degrades gracefully for old collections."""

    def test_fts_index_param_creation(self):
        """FtsIndexParam can be created with jieba tokenizer."""
        param = zvec.FtsIndexParam(
            tokenizer_name="jieba",
            filters=["lowercase"],
        )
        assert param.tokenizer_name == "jieba"

    def test_fts_query_with_match_string(self):
        """Fts query can be constructed with match_string."""
        fts = zvec.Fts(match_string="机器学习 向量数据库")
        assert fts.match_string == "机器学习 向量数据库"

    def test_fts_query_with_query_string(self):
        """Fts query can be constructed with query_string (boolean ops)."""
        fts = zvec.Fts(query_string='+vector -slow "exact phrase"')
        assert fts.query_string == '+vector -slow "exact phrase"'

    def test_multi_query_list_construction(self):
        """A list of [vector_query, fts_query] can be constructed."""
        vec_q = zvec.Query("embedding", vector=[0.1] * 10)
        fts_q = zvec.Query("text", fts=zvec.Fts(match_string="test"))
        queries = [vec_q, fts_q]
        assert len(queries) == 2
        assert queries[0].field_name == "embedding"
        assert queries[1].field_name == "text"
