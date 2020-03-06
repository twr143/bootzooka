package template.util

import java.time.Clock

import template.config.Config

trait BaseModule {
  def idGenerator: IdGenerator
  def clock: Clock
  def config: Config
}
