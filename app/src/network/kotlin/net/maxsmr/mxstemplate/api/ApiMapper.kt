package net.maxsmr.mxstemplate.api

import net.maxsmr.core_network.model.request.api.IApiMapper

object ApiMapper : IApiMapper {

    override val map = mutableMapOf<IApiMapper.ApiKeyInfo, IApiMapper.ApiValueInfo>()
}