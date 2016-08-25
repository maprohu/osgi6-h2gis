package osgi6.h2gis.impl

import java.io.File
import javax.sql.DataSource

import ogsi6.libs.h2gis.H2GisUtil
import org.h2.Driver
import osgi6.common.BaseActivator
import osgi6.h2gis.H2GisApi
import osgi6.h2gis.H2GisApi.{ClosableDataSource, Provider}
import osgi6.multi.api.{Context, ContextApi, ContextApiTrait}
import rx.Var


/**
  * Created by pappmar on 19/07/2016.
  */
class H2GisActivator extends BaseActivator(
  { ctx =>
    val ctxRx = Var(Option.empty[Context])

    val ctxReg = ContextApi.registry.listen(
      new ContextApiTrait.Handler {
        override def dispatch(ctx: Context): Unit = {
          ctxRx() = Option(ctx)
        }
      }
    )

    import rx.Ctx.Owner.Unsafe.Unsafe
    val folded = ctxRx.fold(() => ())({ (removeOld, newCtx) =>
      removeOld()

      newCtx.map({ ctx =>
        val unset = H2GisActivator.activate(ctx)

        { () =>
          unset.remove
        }
      }).getOrElse(() => ())
    })

    { () =>
      ctxReg.remove

      folded.kill()

      Driver.unload()

      ()
    }
  }
)

object H2GisActivator {

  def activate(ctx: Context) = {
    H2GisApi.registry.set(new Provider {
      override def create(): ClosableDataSource = synchronized {
        createDataSourceFromContext(ctx)
      }
    })
  }

  def createDataSourceFromContext(ctx: Context) = {
    val dbFile = new File(ctx.data.getParentFile, s"storage/${ctx.name}/h2gis/h2gis")
    val (ds, dbClose) = H2GisUtil.createDataSource(dbFile)

    new ClosableDataSource {
      override def dataSource(): DataSource = ds
      override def close(): Unit = dbClose()
    }
  }




}

