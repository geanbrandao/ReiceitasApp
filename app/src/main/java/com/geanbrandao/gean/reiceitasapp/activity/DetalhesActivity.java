package com.geanbrandao.gean.reiceitasapp.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.geanbrandao.gean.reiceitasapp.R;
import com.geanbrandao.gean.reiceitasapp.bdOffline.ControleBD;
import com.geanbrandao.gean.reiceitasapp.conexao.Yummly;
import com.geanbrandao.gean.reiceitasapp.helper.MelhoraImagem;
import com.geanbrandao.gean.reiceitasapp.helper.ReceitasFavoritas;
import com.geanbrandao.gean.reiceitasapp.json.ReceitaDetalhes;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class DetalhesActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView mDetNome, mDetCategoria, mDetIngredientes, mDetModoPreparo, mTempoTotal, mPorcoes ;
    private Button mVoltar, mFavorito;
    private Button mVisitarSite;
    private ReceitaDetalhes detalhes;
    private ControleBD crud;
    private Cursor cursor;
    private String idReceitaAtual;
    private String listaIngredientesFormatada;
    private Bundle bundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhes);

        imageView = findViewById(R.id.iv_det_receita_off);
        mDetNome = findViewById(R.id.tv_det_nome_off);
        mDetCategoria = findViewById(R.id.tv_det_categorias_off);
        mDetIngredientes = findViewById(R.id.tv_ingredientes);
        mVoltar = findViewById(R.id.b_voltar_fav_off);
        mVisitarSite = findViewById(R.id.b_acessar_site);
        mFavorito = findViewById(R.id.ib_favorito);
        mDetModoPreparo = findViewById(R.id.tv_modo_preparo);
        mPorcoes = findViewById(R.id.tv_porcoes);
        mTempoTotal = findViewById(R.id.tv_tempo_total);


        Yummly y = new Yummly(getResources().getString(R.string.appId),
                getResources().getString(R.string.appKey));

        detalhes = new ReceitaDetalhes();


        // pega as informacoes da activity anterior
        bundle = getIntent().getExtras();
        if (bundle != null) {

            idReceitaAtual = bundle.getString("id");
            StringBuilder builder = new StringBuilder();
            for (int aux = 0; aux < bundle.getInt("quantidadeIngredientes"); aux++) {
                builder.append("- ");
                builder.append(bundle.getString("ing" + aux));
                if (aux < bundle.getInt("quantidadeIngredientes") - 1) {
                    builder.append("\n");
                }
            }
            mDetIngredientes.setText(builder);
            listaIngredientesFormatada = builder.toString();
            Glide.with(this).load(bundle.getString("smallImageUrls"))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        }



        try {
            detalhes = y.getReceitaDetalhes(idReceitaAtual);

            mTempoTotal.setText(detalhes.getTotalTime());
            mPorcoes.setText(detalhes.getYield());
            mDetModoPreparo.setText(formataTextoModoPreparo(detalhes.getIngredientLines()));
            mDetNome.setText(detalhes.getName());
            mDetCategoria.setText(detalhes.getSource().getSourceDisplayName());
        } catch (Exception e) {
            Log.i("RetornoApi", "Erro Detalhes " + e);
            e.printStackTrace();
        }

        // verifica se ja eh favorito
        crud = new ControleBD(getBaseContext());
        cursor = crud.read(idReceitaAtual);
        if (cursor.getCount() > 0) {
            // vai marcar a receita como favorita
            Log.i("Database", "Achou o id no database");
            mFavorito.setBackground(getDrawable(R.drawable.ic_action_star_yellow));
        }

        mVoltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mVisitarSite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (detalhes.getSource().getSourceSiteUrl() != null) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(detalhes.getSource().getSourceRecipeUrl()));
                    startActivity(i);
                }
            }
        });

        mFavorito.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // verifica se ja eh favorito
                crud = new ControleBD(getBaseContext());
                cursor = crud.read(idReceitaAtual);
                if (cursor.getCount() > 0) {
                    // vai marcar a receita como favorita
                    Log.i("Database", "Achou o id no database");
                    long res = crud.deletaReceita(idReceitaAtual);
                    if(res != -1) {
                        Log.i("Database", "Favorito deletado do banco de dados");
                        mFavorito.setBackground(getDrawable(R.drawable.ic_action_star_border_black));
                    } else {
                        mFavorito.setBackground(getDrawable(R.drawable.ic_action_star_yellow));
                        Log.i("Database", "Falha ao deletar favorito");
                    }

                } else {
                    // pega a imagem e transforma em byte[]
                    Drawable drawable = imageView.getDrawable();
                    Bitmap bitmap = MelhoraImagem.drawableToBitmap(drawable);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    crud = new ControleBD(getBaseContext());
                    long resultado = crud.insert(idReceitaAtual,
                            listaIngredientesFormatada,
                            mDetNome.getText().toString(),
                            mDetCategoria.getText().toString(),
                            detalhes.getRating(),
                            byteArray,
                            mPorcoes.getText().toString(),
                            mDetModoPreparo.getText().toString(),
                            mTempoTotal.getText().toString(),
                            detalhes.getSource().getSourceRecipeUrl()
                    );

                    if (resultado != -1) {
                        Log.i("Database", "favorito inserido");
                        mFavorito.setBackground(getDrawable(R.drawable.ic_action_star_yellow));
                    } else {
                        Log.i("Database", "falha ao inserir favorito");
                    }
                }
            }
        });

    }

    public String formataTextoModoPreparo(List<String> list) {

        StringBuilder builder = new StringBuilder();
        int i = 0;
        for(String s: list) {
            builder.append("- ");
            builder.append(s);
            if (i < list.size()-1) {
                builder.append("\n");
            }
            ++i;
        }

        return builder.toString();
    }

    @Override
    public void finish() {
        super.finish();
    }
}
