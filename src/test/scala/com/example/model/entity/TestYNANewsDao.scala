package com.example.model.entity

import com.example.model.persistent.table.YNANews.YNANewsRecord
import com.example.model.repository.{CommonRDB, YNANewsDao}
import com.example.service.news.YNAParser.YNASection
import org.joda.time.DateTime

class TestYNANewsDao extends CommonDaoCRUDTest[YNANewsRecord] {
  override def getDao: CommonRDB[YNANewsRecord] = YNANewsDao.getInstance

  override def getCreateExampleDataSet(exampleDataSetCount: Int): Vector[YNANewsRecord] = {
    (1 to exampleDataSetCount).map(k =>
      YNANewsRecord(s"newsURL-$k", YNASection.politics, s"title-$k", s"contents-$k", DateTime.now, DateTime.now)
    ).toVector
  }

  override def getExampleRecord: YNANewsRecord = {
    YNANewsRecord(s"newsURL-99", YNASection.politics, s"title-98", s"contents-98", DateTime.now, DateTime.now)
  }

  override def getExampleRecordUpdated: YNANewsRecord = {
    YNANewsRecord(s"newsURL-99", YNASection.sports, s"title-99", s"contents-99", DateTime.now, DateTime.now)
  }
}
